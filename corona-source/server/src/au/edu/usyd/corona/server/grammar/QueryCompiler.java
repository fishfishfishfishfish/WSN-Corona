package au.edu.usyd.corona.server.grammar;


import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

import au.edu.usyd.corona.grammar.TokenParseException;
import au.edu.usyd.corona.middleLayer.TimeSync;
import au.edu.usyd.corona.scheduler.ChildResultStore;
import au.edu.usyd.corona.scheduler.QueryTask;
import au.edu.usyd.corona.scheduler.SchedulableTask;
import au.edu.usyd.corona.scheduler.TaskID;
import au.edu.usyd.corona.sensing.SenseManager;
import au.edu.usyd.corona.server.srdb.BaseForwardOperator;
import au.edu.usyd.corona.srdb.*;
import au.edu.usyd.corona.types.FloatType;
import au.edu.usyd.corona.types.IEEEAddressType;
import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.types.LongType;
import au.edu.usyd.corona.types.ValueType;
import au.edu.usyd.corona.util.ClassIdentifiers;

/**
 * This class compiles queries that are normal sense-based queries as opposed to
 * the other types of queries that our grammar supports.
 * 
 * @author Tim Dawborn
 * @author Raymes Khoury
 * @version 4.0
 */
class QueryCompiler extends QLPacketTypeCompiler<QueryTask> {
	public static final int DEFAULT_RUNCOUNT = 1;
	public static final long DEFAULT_EPOCH = 5000;
	
	private static final Map<String, Byte> ALL_AGGREGATES;
	private static final Map<String, Byte> COLUMN_NUMBERS;
	
	private final LinkedHashMap<String, ColumnAttribute> selectColumns = new LinkedHashMap<String, ColumnAttribute>();
	private final LinkedHashMap<String, ColumnAttribute> whereColumns = new LinkedHashMap<String, ColumnAttribute>();
	private final LinkedHashMap<String, ColumnAttribute> havingColumns = new LinkedHashMap<String, ColumnAttribute>();
	private final LinkedHashMap<String, ColumnAttribute> groupByColumns = new LinkedHashMap<String, ColumnAttribute>();
	private final LinkedHashMap<ColumnAttribute, Byte> networkColumnNumbers = new LinkedHashMap<ColumnAttribute, Byte>(); // mapping from column attribute to i, i being the ith column in the network table
	private final List<ColumnAttribute> aggregates = new ArrayList<ColumnAttribute>();
	
	private boolean needsCount; // whether or not we need the count column kept
	
	private byte[] networkSchema; // the schema used in the network
	private byte[] baseSchema; // the schema used at the base
	
	private int runcount = DEFAULT_RUNCOUNT; // the RUNCOUNT of the query
	private long epoch = DEFAULT_EPOCH; // the EPOCH of the query 
	private long starttime = -1; // the STARTTIME of the query
	private boolean starttimeIsRelative = false;
	
	private int whereChild = -1; // the index of the child of the WHERE clause in the root tree
	private int havingChild = -1; // the index of the child of the HAVING clause in the root tree
	
	private ConditionExpression whereClauseCondition = null;
	private ConditionExpression havingClauseCondition = null;
	
	static {
		// map of all the aggreate functions to their byte value used in SRDB
		ALL_AGGREGATES = new LinkedHashMap<String, Byte>();
		String[] names = AggregateOperator.FUNCTIONS;
		for (byte i = 0; i != names.length; i++)
			ALL_AGGREGATES.put(names[i], i);
		
		// map of the column numbers to the column they appear in returned from a SENSE table 
		COLUMN_NUMBERS = new LinkedHashMap<String, Byte>();
		names = SenseManager.getInstance().getColumnNames();
		for (byte i = 0; i != names.length; i++)
			COLUMN_NUMBERS.put(names[i].toUpperCase(), i);
	}
	
	/**
	 * Internal class used for storing the properties of columns required by the
	 * query
	 */
	private class ColumnAttribute {
		public final String attribute;
		public final byte col;
		public final byte aggregate;
		
		public ColumnAttribute(String name, String aggregate) {
			this.attribute = name;
			this.col = COLUMN_NUMBERS.get(name);
			this.aggregate = (aggregate == null) ? -1 : ALL_AGGREGATES.get(aggregate);
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof ColumnAttribute))
				return false;
			ColumnAttribute a = (ColumnAttribute) o;
			return a.aggregate == aggregate && a.col == col;
		}
		
		@Override
		public int hashCode() {
			return (aggregate << 8) | col;
		}
		
		@Override
		public String toString() {
			return "CA(" + attribute + " " + aggregate + ")";
		}
	}
	
	public QueryCompiler(Tree root, int queryId) {
		super(root, queryId);
	}
	
	private ConditionExpression makeAND(ConditionExpression a, ConditionExpression b) {
		ConditionExpression c1 = new NANDExpression(a, b);
		return new NANDExpression(c1, c1);
	}
	
	private ConditionExpression makeOR(ConditionExpression a, ConditionExpression b) {
		ConditionExpression c1 = new NANDExpression(a, a);
		ConditionExpression c2 = new NANDExpression(b, b);
		return new NANDExpression(c1, c2);
	}
	
	private ConditionExpression makeNOT(ConditionExpression a) {
		return new NANDExpression(a, a);
	}
	
	private ConditionExpression makeLT(ConditionExpression a, ConditionExpression b) {
		return new LessThanExpression(a, b);
	}
	
	private ConditionExpression makeEQ(ConditionExpression a, ConditionExpression b) {
		return new EqualsExpression(a, b);
	}
	
	private ConditionExpression conditionToSRDBTree(Tree _node, boolean isWhere) throws QLCompileException {
		CommonTree node = (CommonTree) _node;
		String op = node.getText();
		
		if (op.equals("AND")) {
			return makeAND(conditionToSRDBTree(node.getChild(0), isWhere), conditionToSRDBTree(node.getChild(1), isWhere));
		}
		else if (op.equals("OR")) {
			return makeOR(conditionToSRDBTree(node.getChild(0), isWhere), conditionToSRDBTree(node.getChild(1), isWhere));
		}
		else if (op.equals("NOT")) {
			return makeNOT(conditionToSRDBTree(node.getChild(0), isWhere));
		}
		else if (op.equals("<")) {
			return makeLT(conditionToSRDBTree(node.getChild(0), isWhere), conditionToSRDBTree(node.getChild(1), isWhere));
		}
		else if (op.equals("<=")) {
			ConditionExpression c0 = conditionToSRDBTree(node.getChild(0), isWhere);
			ConditionExpression c1 = conditionToSRDBTree(node.getChild(1), isWhere);
			return makeOR(makeLT(c0, c1), makeEQ(c0, c1));
		}
		else if (op.equals("==")) {
			return makeEQ(conditionToSRDBTree(node.getChild(0), isWhere), conditionToSRDBTree(node.getChild(1), isWhere));
		}
		else if (op.equals("!=")) {
			ConditionExpression x = makeEQ(conditionToSRDBTree(node.getChild(0), isWhere), conditionToSRDBTree(node.getChild(1), isWhere));
			return makeNOT(x);
		}
		else if (op.equals(">=")) {
			return makeNOT(makeLT(conditionToSRDBTree(node.getChild(0), isWhere), conditionToSRDBTree(node.getChild(1), isWhere)));
		}
		else if (op.equals(">")) {
			ConditionExpression c0 = conditionToSRDBTree(node.getChild(0), isWhere);
			ConditionExpression c1 = conditionToSRDBTree(node.getChild(1), isWhere);
			return makeAND(makeNOT(makeLT(c0, c1)), makeNOT(makeEQ(c0, c1)));
		}
		else if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
			ConditionExpression c0 = conditionToSRDBTree(node.getChild(0), isWhere);
			ConditionExpression c1 = conditionToSRDBTree(node.getChild(1), isWhere);
			if (op.equals("+"))
				return new AddExpression(c0, c1);
			else if (op.equals("-"))
				return new SubtractExpression(c0, c1);
			else if (op.equals("*"))
				return new MultiplyExpression(c0, c1);
			else
				return new DivideExpression(c0, c1);
		}
		else if (node.getType() == CoronaQLLexer.NUMBER_INT) {
			return new ConstantExpression(new LongType(Long.parseLong(op)));
		}
		else if (node.getType() == CoronaQLLexer.NUMBER_FLOAT) {
			return new ConstantExpression(new FloatType(Float.parseFloat(op)));
		}
		else if (node.getType() == CoronaQLLexer.ADDRESS) {
			return new ConstantExpression(new IEEEAddressType(op));
		}
		else if (node.getType() == CoronaQLLexer.WORD) {
			op = op.toUpperCase();
			if (isWhere)
				return new AttributeExpression(COLUMN_NUMBERS.get(op));
			else
				return new AttributeExpression(networkColumnNumbers.get(havingColumns.get(op)));
		}
		else if (node.getType() == CoronaQLLexer.AGGREGATION) {
			String function = node.getChild(0).getText().toUpperCase() + "_";
			String attribute = node.getChild(1).getText().toUpperCase();
			if (node.getChild(1).getType() != CoronaQLLexer.ASTRIX)
				function += attribute;
			return new AttributeExpression(networkColumnNumbers.get(havingColumns.get(function)));
		}
		else
			throw new QLCompileException("Unknown item found while traversing " + (isWhere ? "WHERE" : "HAVING") + ": " + op + '\n' + node.toStringTree());
	}
	
	private long _extractVerboseTime(Tree node) throws QLCompileException {
		long time = 0;
		for (int i = 0; i < node.getChildCount(); i += 2) {
			int numeric = Integer.parseInt(node.getChild(i).getText());
			int multiplier = 0;
			
			switch (node.getChild(i + 1).getType()) {
			case CoronaQLLexer.SECOND:
				if (numeric < 0 || numeric > 59)
					throw new QLCompileException("Invalid number of seconds: " + numeric);
				multiplier = 1000;
				break;
			case CoronaQLLexer.MINUTE:
				if (numeric < 0 || numeric > 59)
					throw new QLCompileException("Invalid number of minutes: " + numeric);
				multiplier = 1000 * 60;
				break;
			case CoronaQLLexer.HOUR:
				if (numeric < 0)
					throw new QLCompileException("Invalid number of hours: " + numeric);
				multiplier = 1000 * 60 * 60;
				break;
			default:
				throw new QLCompileException("Unknown type of node (" + node.getChild(i + 1).getType() + ") in verbose time '" + ((CommonTree) node).toStringTree() + "'");
			}
			
			time += numeric * multiplier;
		}
		return time;
	}
	
	private long extractEpoch(Tree node) throws QLCompileException {
		return _extractVerboseTime(node);
	}
	
	private long extractStarttime(Tree node) throws QLCompileException {
		CommonTree child = (CommonTree) node.getChild(0);
		if (child.getType() == CoronaQLLexer.AT) {
			int hours = Integer.parseInt(child.getChild(0).getText());
			int minutes = Integer.parseInt(child.getChild(1).getText());
			int seconds = Integer.parseInt(child.getChild(2).getText());
			
			// validate the time
			if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59 || seconds < 0 || seconds > 59)
				throw new QLCompileException("Invalid 24 hours format time '" + hours + ":" + minutes + ":" + seconds + "'");
			
			// create the time asked for
			Calendar c = new GregorianCalendar();
			c.set(Calendar.HOUR_OF_DAY, hours);
			c.set(Calendar.MINUTE, minutes);
			c.set(Calendar.SECOND, seconds);
			
			// if it has an ON DATE component
			if (child.getChildCount() == 4) {
				CommonTree dateNode = (CommonTree) node.getChild(4);
				int day = Integer.parseInt(dateNode.getChild(0).getText());
				int month = Integer.parseInt(dateNode.getChild(1).getText());
				int year = Integer.parseInt(dateNode.getChild(2).getText());
				String date = day + "." + month + "." + year;
				
				// validate the date
				DateFormat f = new SimpleDateFormat("dd.MM.yyyy");
				f.setLenient(false);
				try {
					f.parse(date);
				}
				catch (java.text.ParseException e) {
					throw new QLCompileException("Invalid 'dd.MM.yyyy' formate date given '" + date + "'");
				}
				
				// update our current start time
				c.set(Calendar.DAY_OF_MONTH, day);
				c.set(Calendar.MONTH, month);
				c.set(Calendar.YEAR, year);
			}
			
			starttimeIsRelative = false;
			return c.getTimeInMillis();
		}
		else {
			starttimeIsRelative = true;
			return _extractVerboseTime(child);
		}
	}
	
	private int extractRuncount(Tree node) throws QLCompileException {
		if (node.getChild(0).getType() == CoronaQLLexer.FOREVER) {
			return SchedulableTask.RUNCOUNT_FOREVER;
		}
		else {
			int runcount;
			try {
				runcount = Integer.parseInt(node.getChild(0).getText());
			}
			catch (NumberFormatException e) {
				throw new QLCompileException("Value for RUNCOUNT is too large; must be less than or equal to " + Integer.MAX_VALUE);
			}
			if (runcount <= 0)
				throw new QLCompileException("Value for RUNCOUNT must be a positive integer");
			return runcount;
		}
	}
	
	private void extractAttributes(Tree _node, LinkedHashMap<String, ColumnAttribute> columns, boolean errorDuplicates) throws QLCompileException {
		CommonTree node = (CommonTree) _node;
		if (node.getType() == CoronaQLLexer.WORD) {
			String col = node.getText().toUpperCase();
			
			// checks for errors
			if (errorDuplicates && columns.containsKey(col))
				throw new QLCompileException("Duplicate attribute '" + col + "' found");
			if (!COLUMN_NUMBERS.containsKey(col))
				throw new QLCompileException("Unknown attribute '" + col + "'");
			
			columns.put(col, new ColumnAttribute(col, null));
		}
		else if (node.getType() == CoronaQLLexer.AGGREGATION) {
			// extracts data
			String aggregation = node.getChild(0).getText().toUpperCase();
			String col = node.getChild(1).getText().toUpperCase();
			String key = aggregation + "_";
			if (node.getChild(1).getType() != CoronaQLLexer.ASTRIX)
				key += col;
			
			// checks for errors
			if (errorDuplicates && columns.containsKey(key))
				throw new QLCompileException("Duplicate attribute '" + aggregation + '(' + col + ')' + "' found");
			if (!ALL_AGGREGATES.containsKey(aggregation))
				throw new QLCompileException("Unknown aggregate function '" + aggregation + "'");
			
			if (aggregation.equals("COUNT")) {
				if (!COLUMN_NUMBERS.containsKey(col) && (node.getChild(1).getType() != CoronaQLLexer.ASTRIX))
					throw new QLCompileException("Unknown attribute '" + col + "'");
			}
			else {
				if (!COLUMN_NUMBERS.containsKey(col))
					throw new QLCompileException("Unknown attribute '" + col + "'");
			}
			
			if (node.getChild(1).getType() == CoronaQLLexer.ASTRIX)
				col = "_COUNT";
			ColumnAttribute ca = new ColumnAttribute(col, aggregation);
			columns.put(key, ca);
			
			// updates the need count variable if we need a count
			if (aggregation.equalsIgnoreCase("AVG") || aggregation.equalsIgnoreCase("COUNT"))
				needsCount = true;
			
			// adds it to the global collection of aggregates required
			if (ca.aggregate == AggregateOperator.COUNT) {
				boolean found = false;
				for (ColumnAttribute c : aggregates)
					if (c.aggregate == AggregateOperator.COUNT) {
						found = true;
						break;
					}
				if (!found)
					aggregates.add(ca);
			}
			else {
				if (!aggregates.contains(ca))
					aggregates.add(ca);
			}
		}
		
		// recursive traversal through the tree
		if (node.getType() != CoronaQLLexer.AGGREGATION)
			for (int i = 0; i != node.getChildCount(); i++)
				extractAttributes(node.getChild(i), columns, errorDuplicates);
	}
	
	private TableOperator _compileWhereClause(TableOperator op) throws QLCompileException {
		// if there was no WHERE clause, bypass this step
		if (whereChild == -1)
			return op;
		if (whereClauseCondition == null)
			whereClauseCondition = conditionToSRDBTree(root.getChild(whereChild).getChild(0), true);
		return new SelectionOperator(op, whereClauseCondition);
	}
	
	private TableOperator _compileHavingClause(TableOperator op) throws QLCompileException {
		// if there was no HAVING clause, bypass this step
		if (havingChild == -1)
			return op;
		if (havingClauseCondition == null)
			havingClauseCondition = conditionToSRDBTree(root.getChild(havingChild).getChild(0), false);
		return new SelectionOperator(op, havingClauseCondition);
	}
	
	private TableOperator _compileSelectClause(TableOperator where, boolean networkTree) throws QLCompileException {
		Vector<Byte> cols = new Vector<Byte>();
		
		if (networkTree) {
			for (ColumnAttribute ca : networkColumnNumbers.keySet()) {
				cols.add(ca.col);
			}
		}
		else {
			for (ColumnAttribute ca : selectColumns.values()) {
				if (ca.aggregate == AggregateOperator.COUNT)
					cols.add((byte) 0);
				else
					cols.add(networkColumnNumbers.get(ca));
			}
		}
		
		return new ProjectionOperator(where, cols);
	}
	
	private TableOperator _compileAggregates(TableOperator merged) throws QLCompileException {
		if (groupByColumns.isEmpty() && aggregates.isEmpty())
			return merged;
		
		final byte[] groupBys = new byte[groupByColumns.size()];
		int i = 0;
		for (ColumnAttribute ca : groupByColumns.values())
			groupBys[i++] = networkColumnNumbers.get(ca);
		
		final byte[] functions = new byte[aggregates.size()];
		final byte[] functionCols = new byte[functions.length];
		i = 0;
		for (ColumnAttribute ca : aggregates) {
			functions[i] = ca.aggregate;
			if (ca.aggregate == AggregateOperator.COUNT)
				functionCols[i] = 0;
			else
				functionCols[i] = networkColumnNumbers.get(ca);
			i++;
		}
		
		return new AggregateOperator(merged, functions, functionCols, groupBys);
	}
	
	private TableOperator compileNetworkTree(TaskID taskId, ChildResultStore results, boolean hasAverage) throws QLCompileException {
		TableOperator op = new SenseOperator(taskId); // sense
		op = _compileWhereClause(op); // selection
		op = _compileSelectClause(op, true); // projection
		op = new MergeOperator(taskId, new CollectOperator(taskId, results), op); // merge
		if (!hasAverage)
			op = _compileAggregates(op); // function application (all functions at once)
		op = new ForwardOperator(op, results); // forward
		return op;
	}
	
	private TableOperator compileBaseTree(TaskID taskId, ChildResultStore results, boolean hasAverage) throws QLCompileException {
		TableOperator op = new CollectOperator(taskId, results);
		op = _compileAggregates(op);
		op = _compileHavingClause(op);
		
		Vector<Byte> cols = new Vector<Byte>();
		for (ColumnAttribute ca : selectColumns.values()) {
			if (ca.aggregate == AggregateOperator.COUNT)
				cols.add((byte) 0);
			else
				cols.add(networkColumnNumbers.get(ca));
		}
		op = new ProjectionOperator(op, cols);
		op = new BaseForwardOperator(op, results);
		return op;
	}
	
	private void doInitialPass() throws QLCompileException {
		boolean selectStar = false;
		
		// for each child in the root of the tree
		for (int i = 0; i != root.getChildCount(); i++) {
			Tree child = root.getChild(i);
			
			switch (child.getType()) {
			case CoronaQLLexer.SELECT:
				extractAttributes(child, selectColumns, true);
				if (child.getChild(0).getType() == CoronaQLLexer.ALL_ATTRIBS) { // if its a SELECT * case, handle correctly
					selectStar = true;
					// make sure that its by itself, rather than say 'SELECT *, node'
					if (!selectColumns.isEmpty())
						throw new QLCompileException("'*' found in SELECT clause with other attributes");
					// use all the columns except for _count, as semantically * does not include _count
					for (String col : COLUMN_NUMBERS.keySet())
						if (!col.equalsIgnoreCase("_COUNT"))
							selectColumns.put(col, new ColumnAttribute(col, null));
				}
				break;
			
			case CoronaQLLexer.FROM:
				// do nothing; this syntax is here for future extensions
				break;
			
			case CoronaQLLexer.WHERE:
				extractAttributes(child, whereColumns, false);
				whereChild = i;
				break;
			
			case CoronaQLLexer.HAVING:
				if (selectStar)
					throw new QLCompileException("Cannot SELECT * in a query involving aggregates.");
				extractAttributes(child, havingColumns, false);
				havingChild = i;
				break;
			
			case CoronaQLLexer.GROUPBY:
				if (selectStar)
					throw new QLCompileException("Cannot SELECT * in a query involving aggregates.");
				extractAttributes(child, groupByColumns, true);
				break;
			
			case CoronaQLLexer.RUNCOUNT:
				runcount = extractRuncount(child);
				break;
			
			case CoronaQLLexer.EPOCH:
				epoch = extractEpoch(child);
				break;
			
			case CoronaQLLexer.START:
				starttime = extractStarttime(child);
				break;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void doSemanticChecks() throws QLCompileException {
		// Ensure we select out at least one attribute
		if (selectColumns.isEmpty())
			throw new QLCompileException("No columns found in SELECT statement");
		
		// ensure that there are no aggregates in WHERE clauses
		for (ColumnAttribute c : whereColumns.values())
			if (c.aggregate != -1)
				throw new QLCompileException("Aggregate found in WHERE clause");
		
		// ensure that there are no aggregates in GROUP BY clauses
		for (ColumnAttribute c : groupByColumns.values())
			if (c.aggregate != -1)
				throw new QLCompileException("Aggregate found in GROUP BY clause");
		
		// ensure we don't have an illegal mix of aggregates and non-aggregates in the SELECT clause
		Set<String> aggreateColumns = new HashSet<String>();
		Set<String> nonaggreateColumns = new HashSet<String>();
		
		Map<String, ColumnAttribute>[] tmp = new Map[]{selectColumns, havingColumns};
		for (Map<String, ColumnAttribute> map : tmp) {
			for (Entry<String, ColumnAttribute> e : map.entrySet()) {
				if (e.getValue().aggregate == -1)
					nonaggreateColumns.add(e.getKey());
				else
					aggreateColumns.add(e.getKey());
			}
		}
		if (!aggreateColumns.isEmpty() && !nonaggreateColumns.isEmpty()) {
			for (String c : nonaggreateColumns)
				if (!groupByColumns.containsKey(c))
					throw new QLCompileException("Column '" + c + "' found in aggregate query but not in GROUP BY statement");
		}
		if (aggreateColumns.isEmpty() && !groupByColumns.isEmpty())
			throw new QLCompileException("Can not have a GROUP BY statement with no aggregate given in the SELECT statement");
	}
	
	private void testOperationsAndGetSchemas(boolean hasAverage, TaskID taskID) throws QLCompileException {
		// where clause
		TableOperator testWhere = _compileWhereClause(new ReadOperator(SenseManager.getInstance().sense(taskID)));
		try {
			testWhere.eval(0);
		}
		catch (InvalidOperationException e) {
			throw new QLCompileException("Invalid type operation: " + e);
		}
		
		// Network project and aggregate
		TableOperator testAggregatesAndProject = new ReadOperator(SenseManager.getInstance().sense(taskID));
		if (!hasAverage)
			testAggregatesAndProject = _compileAggregates(testAggregatesAndProject);
		testAggregatesAndProject = _compileSelectClause(testAggregatesAndProject, true);
		Table networkRes = null;
		try {
			networkRes = testAggregatesAndProject.eval(0);
		}
		catch (InvalidOperationException e) {
			throw new QLCompileException("Invalid type operation: " + e);
		}
		
		// Determine network schema
		ValueType[] row = networkRes.getRow(0);
		networkSchema = new byte[row.length];
		for (int i = 0; i < row.length; i++) {
			networkSchema[i] = ClassIdentifiers.getID(row[i].getClass());
		}
		
		// Base project and aggregate
		testAggregatesAndProject = new ReadOperator(networkRes);
		testAggregatesAndProject = _compileAggregates(testAggregatesAndProject);
		testAggregatesAndProject = _compileSelectClause(testAggregatesAndProject, false);
		Table baseRes = null;
		try {
			baseRes = testAggregatesAndProject.eval(0);
		}
		catch (InvalidOperationException e) {
			throw new QLCompileException("Invalid type operation: " + e);
		}
		
		// Determine resultant schema
		row = baseRes.getRow(0);
		baseSchema = new byte[row.length];
		for (int i = 0; i < row.length; i++) {
			baseSchema[i] = ClassIdentifiers.getID(row[i].getClass());
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public QueryTask compile() throws QLCompileException, IOException, TokenParseException {
		TaskID taskID = new TaskID(queryId);
		ChildResultStore results = new ChildResultStore();
		
		// do an initial pass over the tree to extract general information needed when compiling
		doInitialPass();
		
		// check for validity
		doSemanticChecks();
		
		// works out whether the base can do all the function application or not
		boolean hasAverage = false;
		for (ColumnAttribute ca : aggregates)
			if (ca.aggregate == AggregateOperator.AVG)
				hasAverage = true;
		
		// works out the mapping from sense table to network table after projection 
		Set<ColumnAttribute> seen = new HashSet<ColumnAttribute>();
		byte column = 0;
		if (needsCount) {
			ColumnAttribute ca = new ColumnAttribute("_COUNT", "COUNT");
			networkColumnNumbers.put(ca, column++);
		}
		for (Object group : new Object[]{selectColumns, whereColumns, havingColumns, groupByColumns}) {
			for (ColumnAttribute ca : ((Map<String, ColumnAttribute>) group).values()) {
				if (ca.aggregate == AggregateOperator.COUNT)
					continue;
				if (!seen.contains(ca)) {
					seen.add(ca);
					networkColumnNumbers.put(ca, column++);
				}
			}
		}
		
		// compile each tree separately
		TableOperator networkTree = compileNetworkTree(taskID, results, hasAverage);
		TableOperator baseTree = compileBaseTree(taskID, results, hasAverage);
		
		// run against default table to ensure operators are valid
		testOperationsAndGetSchemas(hasAverage, taskID);
		
		// return the corresponding Task object
		final String[] baseAttributes = selectColumns.keySet().toArray(new String[0]);
		
		// if the start time was not set, or if its relative, make it now
		long now = TimeSync.getInstance().getTime();
		if (starttime == -1)
			starttime = now;
		else if (starttimeIsRelative)
			starttime += now;
		return new QueryTask(networkTree, baseTree, starttime, runcount, epoch, taskID, networkSchema, baseSchema, baseAttributes, results);
	}
}
