package au.edu.usyd.corona.grammar;


import java.util.Vector;

import au.edu.usyd.corona.scheduler.QueryTask;
import au.edu.usyd.corona.srdb.*;
import au.edu.usyd.corona.types.BooleanType;
import au.edu.usyd.corona.types.ByteType;
import au.edu.usyd.corona.types.FloatType;
import au.edu.usyd.corona.types.IEEEAddressType;
import au.edu.usyd.corona.types.IntType;
import au.edu.usyd.corona.types.LongType;
import au.edu.usyd.corona.types.ValueType;

/**
 * This class is a parser which converts a simple query language into the
 * corresponding numeric expressions and table operations. If the tokens in the
 * {@link QueryTask} that are requested to be parsed are not valid, then a
 * {@link TokenParseException} is thrown.
 * 
 * @author Tim Dawborn
 * @see TableOperator
 */
public class TokenParser implements TokenGrammarTokens {
	protected TokenLexer lexer = null;
	protected QueryTask queryTask;
	
	/**
	 * Parses a query task into an evaluatable object, which can be directly
	 * executed to run the query
	 * 
	 * @param queryTask the query task to convert into an evaluatable object
	 * @return the corresponding {@link TableOperator} object for the given query
	 * task
	 * @throws TokenParseException if there was an error while processing the
	 * tokens
	 */
	public TableOperator parse(QueryTask queryTask) throws TokenParseException {
		this.queryTask = queryTask;
		lexer = new TokenLexer(queryTask.getTokenStream());
		lexer.next();
		return _parse();
	}
	
	private TableOperator _parse() throws TokenParseException {
		switch (lexer.currentChar()) {
		case T_SENSE:
			return parseSense();
		case T_SELECT:
			return parseSelect();
		case T_FUNCTION:
			return parseFunction();
		case T_PROJECT:
			return parseProject();
		case T_COLLECT:
			return parseCollect();
		case T_FORWARD:
			return parseForward();
		case T_MERGE:
			return parseMerge();
		default:
			throw new TokenParseException("Unknown table operator token type '" + lexer.token() + "' in _parse");
		}
	}
	
	private ConditionExpression condEquals() throws TokenParseException {
		assertDie(T_EQ);
		assertDie(T_GROUP_OPEN);
		ConditionExpression a = _parseConditionTree();
		ConditionExpression b = _parseConditionTree();
		ConditionExpression r = new EqualsExpression(a, b);
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private ConditionExpression condLessThan() throws TokenParseException {
		assertDie(T_LT);
		assertDie(T_GROUP_OPEN);
		ConditionExpression a = _parseConditionTree();
		ConditionExpression b = _parseConditionTree();
		ConditionExpression r = new LessThanExpression(a, b);
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private ConditionExpression condMultiply() throws TokenParseException {
		assertDie(T_MULTIPLY);
		assertDie(T_GROUP_OPEN);
		ConditionExpression a = _parseConditionTree();
		ConditionExpression b = _parseConditionTree();
		ConditionExpression r = new MultiplyExpression(a, b);
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private ConditionExpression condSubtract() throws TokenParseException {
		assertDie(T_SUBTRACT);
		assertDie(T_GROUP_OPEN);
		ConditionExpression a = _parseConditionTree();
		ConditionExpression b = _parseConditionTree();
		ConditionExpression r = new SubtractExpression(a, b);
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private ConditionExpression condAdd() throws TokenParseException {
		assertDie(T_ADD);
		assertDie(T_GROUP_OPEN);
		ConditionExpression a = _parseConditionTree();
		ConditionExpression b = _parseConditionTree();
		ConditionExpression r = new AddExpression(a, b);
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private ConditionExpression condDivide() throws TokenParseException {
		assertDie(T_DIVIDE);
		assertDie(T_GROUP_OPEN);
		ConditionExpression a = _parseConditionTree();
		ConditionExpression b = _parseConditionTree();
		ConditionExpression r = new DivideExpression(a, b);
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private ConditionExpression condNAND() throws TokenParseException {
		assertDie(T_NAND);
		assertDie(T_GROUP_OPEN);
		ConditionExpression a = _parseConditionTree();
		ConditionExpression b = _parseConditionTree();
		ConditionExpression r = new NANDExpression(a, b);
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private ConditionExpression condAttribute() throws TokenParseException {
		assertDie(T_ATTRIBUTE);
		assertDie(T_GROUP_OPEN);
		ConditionExpression r = new AttributeExpression(lexer.currentByte());
		lexer.next();
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	protected ConditionExpression _parseConditionTree() throws TokenParseException {
		switch (lexer.currentChar()) {
		case T_EQ:
			return condEquals();
		case T_LT:
			return condLessThan();
		case T_MULTIPLY:
			return condMultiply();
		case T_DIVIDE:
			return condDivide();
		case T_SUBTRACT:
			return condSubtract();
		case T_ADD:
			return condAdd();
		case T_NAND:
			return condNAND();
		case T_ATTRIBUTE:
			return condAttribute();
		default:
			return new ConstantExpression(_parseValueType());
		}
	}
	
	private ValueType _parseValueType() throws TokenParseException {
		switch (lexer.currentChar()) {
		case T_DATA_TYPE_BOOLEAN:
			return dataTypeBoolean();
		case T_DATA_TYPE_BYTE:
			return dataTypeByte();
		case T_DATA_TYPE_INT:
			return dataTypeInt();
		case T_DATA_TYPE_LONG:
			return dataTypeLong();
		case T_DATA_TYPE_IEEE_ADDRESS:
			return dataTypeIEEEAddress();
		case T_DATA_TYPE_FLOAT:
			return dataTypeFloat();
		case T_DATA_TYPE_7:
			throw new TokenParseException("Custom data type 7 not accounted for");
		case T_DATA_TYPE_8:
			throw new TokenParseException("Custom data type 8 not accounted for");
		case T_DATA_TYPE_9:
			throw new TokenParseException("Custom data type 9 not accounted for");
		case T_DATA_TYPE_10:
			throw new TokenParseException("Custom data type 10 not accounted for");
		case T_DATA_TYPE_11:
			throw new TokenParseException("Custom data type 11 not accounted for");
		case T_DATA_TYPE_12:
			throw new TokenParseException("Custom data type 12 not accounted for");
		case T_DATA_TYPE_13:
			throw new TokenParseException("Custom data type 13 not accounted for");
		case T_DATA_TYPE_14:
			throw new TokenParseException("Custom data type 14 not accounted for");
		case T_DATA_TYPE_15:
			throw new TokenParseException("Custom data type 15 not accounted for");
		default:
			throw new TokenParseException("Unknown valuetype char '" + lexer.currentChar() + "' in _parseValueType");
		}
	}
	
	private TableOperator parseMerge() throws TokenParseException {
		assertDie(T_MERGE);
		assertDie(T_GROUP_OPEN);
		
		TableOperator a = _parse();
		TableOperator b = _parse();
		TableOperator r = new MergeOperator(queryTask.getTaskId(), a, b);
		
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private TableOperator parseForward() throws TokenParseException {
		assertDie(T_FORWARD);
		assertDie(T_GROUP_OPEN);
		
		TableOperator a = _parse();
		TableOperator r = new ForwardOperator(a, queryTask.getChildResults());
		
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private TableOperator parseCollect() throws TokenParseException {
		assertDie(T_COLLECT);
		assertDie(T_GROUP_OPEN);
		TableOperator r = new CollectOperator(queryTask.getTaskId(), queryTask.getChildResults());
		
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private TableOperator parseProject() throws TokenParseException {
		assertDie(T_PROJECT);
		assertDie(T_GROUP_OPEN);
		
		TableOperator a = _parse(); // sense | select | function
		Vector cols = new Vector();
		do {
			cols.addElement(new Byte((byte) lexer.currentLong()));
			lexer.next();
		}
		while (lexer.token() == TokenLexer.TOKEN_INTEGER);
		
		TableOperator r = new ProjectionOperator(a, cols);
		
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private TableOperator parseFunction() throws TokenParseException {
		assertDie(T_FUNCTION);
		assertDie(T_GROUP_OPEN);
		
		TableOperator a = _parse(); // function | select | sense
		
		// the number of functions
		if (lexer.token() != TokenLexer.TOKEN_INTEGER)
			throw new TokenParseException("Found token '" + lexer.currentString() + "' when expected token of type TOKEN_LONG for aggregate number of functions");
		byte numFunctions = lexer.currentByte();
		lexer.next();
		
		// the aggregate functions and their application columns
		final byte[] functions = new byte[numFunctions];
		final byte[] functionColumns = new byte[numFunctions];
		for (byte i = 0; i != numFunctions; i++) {
			if (lexer.token() != TokenLexer.TOKEN_INTEGER)
				throw new TokenParseException("Found token '" + lexer.currentString() + "' when expected token of type TOKEN_LONG for aggregate function");
			functions[i] = lexer.currentByte();
			lexer.next();
			if (lexer.token() != TokenLexer.TOKEN_INTEGER)
				throw new TokenParseException("Found token '" + lexer.currentString() + "' when expected token of type TOKEN_LONG for aggregate function column");
			functionColumns[i] = lexer.currentByte();
			lexer.next();
		}
		
		// the group bys
		if (lexer.token() != TokenLexer.TOKEN_INTEGER)
			throw new TokenParseException("Found token '" + lexer.currentString() + "' when expected token of type TOKEN_LONG for aggregate number of group bys");
		byte numGroupBy = lexer.currentByte();
		lexer.next();
		
		final byte[] groupByColumns = new byte[numGroupBy];
		for (byte i = 0; i != numGroupBy; i++) {
			if (lexer.token() != TokenLexer.TOKEN_INTEGER)
				throw new TokenParseException("Found token '" + lexer.currentString() + "' when expected token of type TOKEN_LONG for aggregate group bys");
			groupByColumns[i] = lexer.currentByte();
			lexer.next();
		}
		
		TableOperator r = new AggregateOperator(a, functions, functionColumns, groupByColumns);
		
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private TableOperator parseSelect() throws TokenParseException {
		assertDie(T_SELECT);
		assertDie(T_GROUP_OPEN);
		
		TableOperator a = _parse(); // sense
		ConditionExpression b = _parseConditionTree(); // the condition tree
		TableOperator r = new SelectionOperator(a, b);
		
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private TableOperator parseSense() throws TokenParseException {
		assertDie(T_SENSE);
		assertDie(T_GROUP_OPEN);
		TableOperator n = new SenseOperator(queryTask.getTaskId());
		assertDie(T_GROUP_CLOSE);
		return n;
	}
	
	private ValueType dataTypeBoolean() throws TokenParseException {
		assertDie(T_DATA_TYPE_BOOLEAN);
		assertDie(T_GROUP_OPEN);
		ValueType r = new BooleanType(lexer.currentInt());
		lexer.next();
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private ValueType dataTypeByte() throws TokenParseException {
		assertDie(T_DATA_TYPE_BYTE);
		assertDie(T_GROUP_OPEN);
		ValueType r = new ByteType(lexer.currentByte());
		lexer.next();
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private ValueType dataTypeInt() throws TokenParseException {
		assertDie(T_DATA_TYPE_INT);
		assertDie(T_GROUP_OPEN);
		ValueType r = new IntType(lexer.currentInt());
		lexer.next();
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private ValueType dataTypeLong() throws TokenParseException {
		assertDie(T_DATA_TYPE_LONG);
		assertDie(T_GROUP_OPEN);
		ValueType r = new LongType(lexer.currentLong());
		lexer.next();
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private ValueType dataTypeIEEEAddress() throws TokenParseException {
		assertDie(T_DATA_TYPE_IEEE_ADDRESS);
		assertDie(T_GROUP_OPEN);
		ValueType r = new IEEEAddressType(lexer.currentLong());
		lexer.next();
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private ValueType dataTypeFloat() throws TokenParseException {
		assertDie(T_DATA_TYPE_FLOAT);
		assertDie(T_GROUP_OPEN);
		ValueType r = new FloatType(lexer.currentFloat());
		lexer.next();
		assertDie(T_GROUP_CLOSE);
		return r;
	}
	
	private void assertDie(char expected) throws TokenParseException {
		if (lexer.currentChar() != expected)
			throw new TokenParseException("Found token '" + lexer.currentChar() + "' when expected '" + expected + "'");
		lexer.next();
	}
}
