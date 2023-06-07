package au.edu.usyd.corona.srdb;


import junit.framework.TestCase;
import au.edu.usyd.corona.types.IntType;
import au.edu.usyd.corona.types.InvalidOperationException;

public class AddExpressionTest extends TestCase {
	
	private void runCombination(int left, int right, int expected) throws InvalidOperationException {
		AddExpression expr = new AddExpression(new ConstantExpression(new IntType(left)), new ConstantExpression(new IntType(right)));
		assertEquals(new IntType(expected), expr.eval(null));
	}
	
	public void testConstants() throws InvalidOperationException {
		runCombination(3, 1, 4);
		runCombination(0, 0, 0);
		runCombination(5, -5, 0);
		runCombination(100, -3, 97);
		runCombination(Integer.MIN_VALUE, Integer.MAX_VALUE, -1);
		runCombination(1, Integer.MAX_VALUE, Integer.MIN_VALUE);
	}
	
}
