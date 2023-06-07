package au.edu.usyd.corona.srdb;


import junit.framework.TestCase;
import au.edu.usyd.corona.types.BooleanType;
import au.edu.usyd.corona.types.IEEEAddressType;
import au.edu.usyd.corona.types.IntType;
import au.edu.usyd.corona.types.LongType;
import au.edu.usyd.corona.types.ValueType;

public class ConstantExpressionTest extends TestCase {
	private static void assertConstant(ValueType value) {
		ConstantExpression e = new ConstantExpression(value);
		assertEquals(value, e.eval(null));
		assertEquals(value.toTokens(), e.toTokens().toString());
	}
	
	public void testConstants() {
		assertConstant(new IntType(0));
		assertConstant(new IntType(Integer.MIN_VALUE));
		assertConstant(new BooleanType(false));
		assertConstant(new LongType(Long.MAX_VALUE));
		assertConstant(new IEEEAddressType(12345678));
	}
}
