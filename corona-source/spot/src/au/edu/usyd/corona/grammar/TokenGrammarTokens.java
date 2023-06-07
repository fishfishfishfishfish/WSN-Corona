package au.edu.usyd.corona.grammar;


/**
 * The tokens used in the token grammar; the grammar that SQL queries are
 * compiled down to
 * 
 * @author Tim Dawborn
 */
public interface TokenGrammarTokens {
	// SRDB operators
	public static final char T_SENSE = 'E';
	public static final char T_SELECT = 'S';
	public static final char T_FUNCTION = 'F';
	public static final char T_PROJECT = 'P';
	public static final char T_COLLECT = 'C';
	public static final char T_MERGE = 'M';
	public static final char T_FORWARD = 'D';
	
	public static final char T_CROSS_PRODUCT = 'X';
	
	// numeric expressions
	public static final char T_EQ = '=';
	public static final char T_LT = '<';
	public static final char T_MULTIPLY = '*';
	public static final char T_DIVIDE = '/';
	public static final char T_SUBTRACT = '-';
	public static final char T_ADD = '+';
	public static final char T_NAND = 'N';
	public static final char T_ATTRIBUTE = 'B';
	
	// grouping
	public static final char T_GROUP_OPEN = '(';
	public static final char T_GROUP_CLOSE = ')';
	
	// table data types
	public static final char T_DATA_TYPE_BYTE = 'a';
	public static final char T_DATA_TYPE_INT = 'b';
	public static final char T_DATA_TYPE_LONG = 'c';
	public static final char T_DATA_TYPE_BOOLEAN = 'd';
	public static final char T_DATA_TYPE_IEEE_ADDRESS = 'e';
	public static final char T_DATA_TYPE_FLOAT = 'f';
	public static final char T_DATA_TYPE_7 = 'g';
	public static final char T_DATA_TYPE_8 = 'h';
	public static final char T_DATA_TYPE_9 = 'i';
	public static final char T_DATA_TYPE_10 = 'j';
	public static final char T_DATA_TYPE_11 = 'k';
	public static final char T_DATA_TYPE_12 = 'l';
	public static final char T_DATA_TYPE_13 = 'm';
	public static final char T_DATA_TYPE_14 = 'n';
	public static final char T_DATA_TYPE_15 = 'o';
}
