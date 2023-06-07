package au.edu.usyd.corona.srdb;


import au.edu.usyd.corona.types.BooleanType;
import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.types.ValueType;

/**
 * This class represents the logical NOT AND (NAND) operation, which compares
 * the results of two numeric expression comparisons, and returns the NAND of
 * these two results.
 * 
 * @author Tim Dawborn
 */
public class NANDExpression extends ConditionExpression {
	public NANDExpression(ConditionExpression child1, ConditionExpression child2) {
		super(child1, child2);
	}
	
	/**
	 * Performs a NAND evaluation on the results of the two inputs (c1 and c2),
	 * specified on construction. It returns 1 or 0 as all other comparison
	 * expressions do
	 */
	public ValueType eval(ValueType[] row) throws InvalidOperationException {
		boolean c1 = !(((BooleanType) children[0].eval(row)).equals(new BooleanType(0)));
		boolean c2 = !(((BooleanType) children[1].eval(row)).equals(new BooleanType(0)));
		return new BooleanType(!(c1 && c2));
	}
	
	public StringBuffer toTokens() {
		StringBuffer b = new StringBuffer();
		b.append(T_NAND).append(T_GROUP_OPEN);
		b.append(children[0].toTokens()).append(' ').append(children[1].toTokens());
		return b.append(T_GROUP_CLOSE);
	}
}
