package au.edu.usyd.corona.srdb;


import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.types.ValueType;

/**
 * This class adds two numeric expressions together.
 */
public class AddExpression extends ConditionExpression {
	public AddExpression(ConditionExpression child1, ConditionExpression child2) {
		super(child1, child2);
	}
	
	/**
	 * Adds two evaluated numeric expressions together and returns the result
	 */
	public ValueType eval(ValueType[] row) throws InvalidOperationException {
		return children[0].eval(row).add(children[1].eval(row));
	}
	
	public StringBuffer toTokens() {
		StringBuffer b = new StringBuffer();
		b.append(T_ADD).append(T_GROUP_OPEN);
		b.append(children[0].toTokens()).append(' ').append(children[1].toTokens());
		return b.append(T_GROUP_CLOSE);
	}
}
