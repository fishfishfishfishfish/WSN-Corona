package au.edu.usyd.corona.srdb;


import au.edu.usyd.corona.types.BooleanType;
import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.types.ValueType;

/**
 * This class evaluates two expressions returning 1 if the first expression is
 * equal to the second else returning 0.
 */
public class EqualsExpression extends ConditionExpression {
	public EqualsExpression(ConditionExpression child1, ConditionExpression child2) {
		super(child1, child2);
	}
	
	/**
	 * Evaluates the less than expression, returning 1 if the first expression is
	 * equal to the second else returning 0.
	 */
	public ValueType eval(ValueType[] row) throws InvalidOperationException {
		return (children[0].eval(row).equals(children[1].eval(row))) ? new BooleanType(1) : new BooleanType(0);
	}
	
	public StringBuffer toTokens() {
		StringBuffer b = new StringBuffer();
		b.append(T_EQ).append(T_GROUP_OPEN);
		b.append(children[0].toTokens()).append(' ').append(children[1].toTokens());
		return b.append(T_GROUP_CLOSE);
	}
}
