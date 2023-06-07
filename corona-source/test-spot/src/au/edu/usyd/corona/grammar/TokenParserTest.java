package au.edu.usyd.corona.grammar;


import junit.framework.TestCase;
import au.edu.usyd.corona.srdb.ConditionExpression;
import au.edu.usyd.corona.types.ByteType;
import au.edu.usyd.corona.types.IntType;
import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.types.LongType;
import au.edu.usyd.corona.types.ValueType;

public class TokenParserTest extends TestCase implements TokenGrammarTokens {
	private TestTokenParser parser;
	
	private class TestTokenParser extends TokenParser {
		public ConditionExpression parseConditionalExpression(String tokens) throws TokenParseException {
			lexer = new TokenLexer(tokens);
			lexer.next();
			return _parseConditionTree();
		}
	}
	
	@Override
	protected void setUp() {
		parser = new TestTokenParser();
	}
	
	private void runNumeric(String tokens, ValueType expected) throws TokenParseException, InvalidOperationException {
		ConditionExpression actual = parser.parseConditionalExpression(tokens);
		assertEquals(expected, actual.eval(null));
	}
	
	public String getByte(byte value) {
		return new StringBuffer().append(T_DATA_TYPE_BYTE).append(T_GROUP_OPEN).append(value).append(T_GROUP_CLOSE).toString();
	}
	
	public String getInt(int value) {
		return new StringBuffer().append(T_DATA_TYPE_INT).append(T_GROUP_OPEN).append(value).append(T_GROUP_CLOSE).toString();
	}
	
	public String getLong(long value) {
		return new StringBuffer().append(T_DATA_TYPE_LONG).append(T_GROUP_OPEN).append(value).append(T_GROUP_CLOSE).toString();
	}
	
	private void runByte(byte value) throws TokenParseException, InvalidOperationException {
		runNumeric(getByte(value), new ByteType(value));
	}
	
	private void runInt(int value) throws TokenParseException, InvalidOperationException {
		runNumeric(getInt(value), new IntType(value));
	}
	
	private void runLong(long value) throws TokenParseException, InvalidOperationException {
		runNumeric(getLong(value), new LongType(value));
	}
	
	public void testConstants() throws TokenParseException, InvalidOperationException {
		runByte((byte) 6);
		runInt(6);
		runLong(6);
		
		runInt(-15297);
	}
	
	public void testPlus() throws TokenParseException, InvalidOperationException {
		StringBuffer tokens = new StringBuffer().append(T_ADD).append(T_GROUP_OPEN).append(getInt(5)).append(' ').append(getByte((byte) 20)).append(T_GROUP_CLOSE);
		runNumeric(tokens.toString(), new IntType(25));
		
		tokens = new StringBuffer().append(T_ADD).append(T_GROUP_OPEN).append(getInt(-647823)).append(' ').append(T_ADD).append(T_GROUP_OPEN).append(getInt(20)).append(' ').append(getInt(8892)).append(T_GROUP_CLOSE).append(T_GROUP_CLOSE);
		runNumeric(tokens.toString(), new IntType(-638911));
		
		tokens = new StringBuffer().append(T_ADD).append(T_GROUP_OPEN).append(getInt(4)).append(' ').append(getInt(-3)).append(T_GROUP_CLOSE);
		runNumeric(tokens.toString(), new IntType(1));
	}
	/*
	 * public void testStar() throws TokenParseException,
	 * InvalidOperationException { runNumeric("*(2 +(-678324 75894759))",
	 * 150432870); runNumeric("*(-3 *(-3 * (-3 1)))", -27); } public void
	 * testEquals() throws TokenParseException, InvalidOperationException {
	 * runNumeric("=(=(8 8) 1)", 1); runNumeric("=(=(4 16) 1)", 0);
	 * runNumeric("=(-10 -10)", 1); runNumeric("=(*(1 0) =(=(1 1) =(0 1)))", 1);
	 * } public void testLessThan() throws TokenParseException,
	 * InvalidOperationException { runNumeric("<(4 3)", 0); runNumeric("<(3 4)",
	 * 1); runNumeric("<(" + (Long.MIN_VALUE) + " " + (Long.MIN_VALUE + 1) + ")",
	 * 1); } public void testNAND() throws TokenParseException,
	 * InvalidOperationException { runNumeric("N(0 0)", 1); runNumeric("N(0 1)",
	 * 1); runNumeric("N(1 0)", 1); runNumeric("N(1 1)", 0);
	 * runNumeric("N(1 25)", 0); }
	 */

}
