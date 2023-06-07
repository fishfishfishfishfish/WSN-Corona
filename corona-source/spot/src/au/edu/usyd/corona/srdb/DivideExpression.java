package au.edu.usyd.corona.srdb;


import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.types.ValueType;
import au.edu.usyd.corona.util.SPOTTools;

/**
 * This class adds two numeric expressions together.
 */
public class DivideExpression extends ConditionExpression {
	public DivideExpression(ConditionExpression child1, ConditionExpression child2) {
		super(child1, child2);
	}
	
	/**
	 * Adds two evaluated numeric expressions together and returns the result
	 */
	public ValueType eval(ValueType[] row) throws InvalidOperationException {
		ValueType lhs = children[0].eval(row);
		ValueType rhs = children[1].eval(row);
		try {
			return lhs.divide(rhs);
		}
		catch (ArithmeticException e) {
			try {
				return ((ValueType) lhs.add(rhs).getClass().newInstance());
			}
			catch (IllegalAccessException e1) {
				SPOTTools.reportError(e1);
			}
			catch (InstantiationException e1) {
				SPOTTools.reportError(e1);
			}
			return null; // this should never happen
		}
	}
	
	public StringBuffer toTokens() {
		StringBuffer b = new StringBuffer();
		b.append(T_ADD).append(T_GROUP_OPEN);
		b.append(children[0].toTokens()).append(' ').append(children[1].toTokens());
		return b.append(T_GROUP_CLOSE);
	}
}
