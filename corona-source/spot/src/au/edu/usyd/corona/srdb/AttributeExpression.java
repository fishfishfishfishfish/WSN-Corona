package au.edu.usyd.corona.srdb;


import au.edu.usyd.corona.types.ValueType;

/**
 * This class provides the means for accessing attributes in a table
 */
public class AttributeExpression extends ConditionExpression {
	private final byte index; //the position in the row of the required attribute
	
	/**
	 * Constructor for an attribute of a table expression, where the column
	 * number is provided
	 * 
	 * @param number the actual column that the required attribute appears in
	 */
	public AttributeExpression(byte number) {
		super(null, null);
		index = number;
	}
	
	/**
	 * Evaluates the attribute expression and returns the value of that attribute
	 */
	public ValueType eval(ValueType[] row) {
		return row[index];
	}
	
	public StringBuffer toTokens() {
		StringBuffer b = new StringBuffer();
		return b.append(T_ATTRIBUTE).append(T_GROUP_OPEN).append(index).append(T_GROUP_CLOSE);
	}
}
