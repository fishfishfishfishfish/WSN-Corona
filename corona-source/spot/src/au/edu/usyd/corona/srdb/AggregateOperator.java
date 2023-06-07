package au.edu.usyd.corona.srdb;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.types.ValueType;

/**
 * This class performs aggregation functions SUM, AVG, MIN, MAX and COUNT over a
 * given attribute from the table passed in. Also groups rows by a given
 * attribute.
 * 
 * @author Tim Dawborn
 * @author Raymes Khoury
 */
public class AggregateOperator extends TableOperator {
	public static final byte SUM = 0;
	public static final byte AVG = 1;
	public static final byte MIN = 2;
	public static final byte MAX = 3;
	public static final byte COUNT = 4;
	public static final String[] FUNCTIONS = {"SUM", "AVG", "MIN", "MAX", "COUNT"};
	
	private final byte[] functions;
	private final byte[] functionColumns;
	private final byte[] groupByColumns;
	
	/**
	 * Constructor for the aggregation. Takes a child operation to be evaluated
	 * as a table.
	 * 
	 * @param child The child table to be evaluated to a table
	 * @param functions
	 * @param functionColumns
	 * @param groupByColumns
	 */
	public AggregateOperator(TableOperator child, byte[] functions, byte[] functionColumns, byte[] groupByColumns) {
		children = new TableOperator[]{child};
		this.functions = functions;
		this.functionColumns = functionColumns;
		this.groupByColumns = (groupByColumns == null) ? new byte[0] : groupByColumns;
	}
	
	private Vector filter(Enumeration rows, int index) throws InvalidOperationException {
		// recursive base case; we have split items into buckets fully
		if (index == groupByColumns.length) {
			return doMagic(rows);
		}
		
		// do the filtering of the current attribute into buckets
		Hashtable output = new Hashtable();
		while (rows.hasMoreElements()) {
			ValueType[] row = (ValueType[]) rows.nextElement();
			ValueType key = row[groupByColumns[index]];
			if (!output.containsKey(key))
				output.put(key, new Vector());
			((Vector) output.get(key)).addElement(row);
		}
		
		// for each bucket, do the next layer of filtering
		Vector outputRows = new Vector();
		for (Enumeration e = output.keys(); e.hasMoreElements();) {
			final Vector value = (Vector) output.get(e.nextElement());
			final Vector done = filter(value.elements(), index + 1);
			for (Enumeration e2 = done.elements(); e2.hasMoreElements();)
				outputRows.addElement(e2.nextElement());
		}
		
		return outputRows;
	}
	
	private Vector doMagic(Enumeration rows) throws InvalidOperationException {
		ValueType[] result = null;
		ValueType[] first = null;
		
		// for each row in the table
		while (rows.hasMoreElements()) {
			ValueType[] row = (ValueType[]) rows.nextElement();
			
			// keep a store of the first row, and create a new results row
			if (first == null) {
				first = row;
				result = new ValueType[row.length];
			}
			
			// for every aggregate we need to apply simultaneously
			boolean usedCount = false;
			for (int i = 0; i != functions.length; i++) {
				final byte col = functionColumns[i];
				
				// if its the first row, copy across the values required
				if (result[col] == null) {
					result[col] = row[col];
					if ((functions[i] == AVG || functions[i] == COUNT) && !usedCount) {
						result[0] = row[0];
						usedCount = true;
					}
				}
				else {
					// do the appropriate actions for each aggregate function
					switch (functions[i]) {
					case AVG:
						result[col] = result[col].add(row[col]);
						break;
					
					case MAX:
						if (result[col].less(row[col]))
							result[col] = row[col];
						break;
					
					case MIN:
						if (!result[col].less(row[col]))
							result[col] = row[col];
						break;
					
					case SUM:
						result[col] = result[col].add(row[col]);
						break;
					}
					
					// ensure we only process the count column once
					if ((functions[i] == AVG || functions[i] == COUNT) && !usedCount) {
						result[0] = result[0].add(row[0]);
						usedCount = true;
					}
				}
			}
		}
		
		Vector res = new Vector();
		if (result != null) {
			// handle any final steps of AVERAGE's at the end here
			if (Network.getInstance().getMode() != Network.MODE_SPOT) {
				for (int i = 0; i != functions.length; i++) {
					if (functions[i] == AVG)
						result[functionColumns[i]] = result[functionColumns[i]].divide(result[0]);
				}
			}
			
			// fill in the blanks not touched by the aggregates
			for (int i = 0; i != result.length; i++)
				if (result[i] == null)
					result[i] = first[i];
			
			res.addElement(result);
		}
		return res;
	}
	
	/**
	 * Actually performs the required aggregation and functionality. This method
	 * is inherited from TableOperator, so see the documentation for that class
	 * for more details. This method runs in O(n) linear time relative to the
	 * size of the table operating on.
	 */
	public Table eval(int epoch) throws InvalidOperationException {
		// evaluates subtree
		Table t = children[0].eval(epoch);
		
		Vector outputRows = filter(t.elements(), 0);
		
		Table output = new Table(t.getTaskID());
		for (Enumeration e = outputRows.elements(); e.hasMoreElements();)
			output.addRow((ValueType[]) e.nextElement());
		
		return output;
	}
	
	public StringBuffer toTokens() {
		StringBuffer b = new StringBuffer();
		b.append(T_FUNCTION).append(T_GROUP_OPEN).append(children[0].toTokens());
		
		b.append(' ').append(functions.length).append(' ');
		for (int i = 0; i != functions.length; i++)
			b.append(functions[i]).append(' ').append(functionColumns[i]).append(' ');
		
		b.append(groupByColumns.length);
		for (int i = 0; i != groupByColumns.length; i++)
			b.append(' ').append(groupByColumns[i]);
		
		return b.append(T_GROUP_CLOSE);
	}
}
