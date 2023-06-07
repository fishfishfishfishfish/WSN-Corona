package au.edu.usyd.corona.srdb;


import junit.framework.TestCase;
import au.edu.usyd.corona.types.BooleanType;
import au.edu.usyd.corona.types.IntType;

public class NANDExpressionTest extends TestCase {
	public void testNANDGreaterThan() {
		
		LessThanExpression lt = new LessThanExpression(new ConstantExpression(new IntType(1)), new ConstantExpression(new IntType(1)));
		
		EqualsExpression eq = new EqualsExpression(new ConstantExpression(new IntType(1)), new ConstantExpression(new IntType(1)));
		
		NANDExpression n1 = new NANDExpression(lt, lt); // true
		NANDExpression n2 = new NANDExpression(eq, eq); // false
		NANDExpression n3 = new NANDExpression(new ConstantExpression(new BooleanType(1)), new ConstantExpression(new BooleanType(0))); //true
		
		try {
			assertEquals(lt.eval(new IntType[0]).equals(new IntType(0)), true);
			assertEquals(eq.eval(new IntType[0]).equals(new IntType(1)), true);
			assertEquals(n1.eval(new IntType[0]).equals(new IntType(1)), true);
			assertEquals(n2.eval(new IntType[0]).equals(new IntType(0)), true);
			assertEquals(n3.eval(new IntType[0]).equals(new IntType(1)), true);
		}
		catch (Exception e) {
		}
		
		NANDExpression sub = //
		new NANDExpression( //
		new NANDExpression( //
		new LessThanExpression( //
		new ConstantExpression(new IntType(1)), //
		new ConstantExpression(new IntType(1)) //
		), //
		new LessThanExpression( //
		new ConstantExpression(new IntType(1)), //
		new ConstantExpression(new IntType(1)) //
		) //
		), //
		new NANDExpression( //
		new EqualsExpression( //
		new ConstantExpression(new IntType(1)), //
		new ConstantExpression(new IntType(1)) //
		), //
		new EqualsExpression( //
		new ConstantExpression(new IntType(1)), //
		new ConstantExpression(new IntType(1)) //
		) //
		)//
		);//
		NANDExpression expr = new NANDExpression(sub, sub);
		try {
			assertEquals(sub.eval(new IntType[0]).equals(new IntType(1)), true);
			assertEquals(expr.eval(new IntType[0]).equals(new IntType(0)), true);
		}
		catch (Exception e) {
		}
		
	}
}
