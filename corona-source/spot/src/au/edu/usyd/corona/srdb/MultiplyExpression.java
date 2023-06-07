package au.edu.usyd.corona.srdb;


import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.types.ValueType;

/**
 * This class multiplies two numeric expressions together.
 */
public class MultiplyExpression extends ConditionExpression {
	public MultiplyExpression(ConditionExpression child1, ConditionExpression child2) {
		super(child1, child2);
	}
	
	/**
	 * Multiplies two evaluated numeric expressions together and returns the
	 * result
	 */
	public ValueType eval(ValueType[] row) throws InvalidOperationException {
		return children[0].eval(row).multiply(children[1].eval(row));
	}
	
	public StringBuffer toTokens() {
		StringBuffer b = new StringBuffer();
		b.append(T_MULTIPLY).append(T_GROUP_OPEN);
		b.append(children[0].toTokens()).append(' ').append(children[1].toTokens());
		return b.append(T_GROUP_CLOSE);
	}
}
