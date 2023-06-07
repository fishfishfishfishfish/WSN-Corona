package au.edu.usyd.corona.server.grammar;


import au.edu.usyd.corona.scheduler.QueryTask;
import au.edu.usyd.corona.scheduler.SchedulableTask;
import au.edu.usyd.corona.scheduler.Scheduler;

public class QueryCompilerTokenGenerationTest extends CompilerTestCase {
	
	private void run(String query, String expectedNetworkTokens, String expectedBaseTokens) throws QLParseException, QLCompileException {
		SchedulableTask q = QLCompiler.getInstance().compile(query, 0, adminUser);
		Scheduler.getInstance().addTask(q);
		
		assertNotNull(q);
		assertTrue(q instanceof QueryTask);
		
		QueryTask qt = (QueryTask) q;
		assertEquals(expectedNetworkTokens, qt.getTokenStream());
		assertEquals("D(" + expectedBaseTokens + ")", qt.getBaseTree().toTokens().toString());
	}
	
	private void runParseError(String query) throws QLCompileException {
		try {
			QLCompiler.getInstance().compile(query, 0, adminUser);
			fail("Parse error should be thrown");
		}
		catch (QLParseException e) {
		}
	}
	
	private void runCompileError(String query) throws QLParseException {
		try {
			QLCompiler.getInstance().compile(query, 0, adminUser);
			fail("Compile error should be thrown");
		}
		catch (QLCompileException e) {
		}
	}
	
	public void testSelect() throws QLParseException, QLCompileException {
		run("SELECT *", //
		"D(M(C() P(E() 1 2 3 4 5 6 7 8 9 10 11 12 13)))", //
		"P(C() 0 1 2 3 4 5 6 7 8 9 10 11 12)");
		run("SELECT node", //
		"D(M(C() P(E() 1)))", //
		"P(C() 0)");
		run("SELECT node, parent", //
		"D(M(C() P(E() 1 10)))", //
		"P(C() 0 1)");
		run("SELECT z, y, x, sw2", //
		"D(M(C() P(E() 5 4 3 7)))", //
		"P(C() 0 1 2 3)");
		
		runCompileError("SELECT node, fish");
	}
	
	public void testSelectAgg() throws QLParseException, QLCompileException {
		run("SELECT MAX(light)", //
		"D(F(M(C() P(E() 8)) 1 3 0 0))",//
		"P(F(C() 1 3 0 0) 0)");
		run("SELECT MAX(light) GROUP BY node", //
		"D(F(M(C() P(E() 8 1)) 1 3 0 1 1))", //
		"P(F(C() 1 3 0 1 1) 0)");
		run("SELECT MAX(light) GROUP BY parent, battery", //
		"D(F(M(C() P(E() 8 10 11)) 1 3 0 2 1 2))", //
		"P(F(C() 1 3 0 2 1 2) 0)");
		
		run("SELECT SUM(light)", //
		"D(F(M(C() P(E() 8)) 1 0 0 0))", //
		"P(F(C() 1 0 0 0) 0)");
		run("SELECT AVG(light)", //
		"D(M(C() P(E() 0 8)))", //
		"P(F(C() 1 1 1 0) 1)");
		run("SELECT MIN(light)", //
		"D(F(M(C() P(E() 8)) 1 2 0 0))", //
		"P(F(C() 1 2 0 0) 0)");
		run("SELECT COUNT(light)", //
		"D(F(M(C() P(E() 0)) 1 4 0 0))", //
		"P(F(C() 1 4 0 0) 0)");
		run("SELECT COUNT(*)", //
		"D(F(M(C() P(E() 0)) 1 4 0 0))", //
		"P(F(C() 1 4 0 0) 0)");
		run("SELECT COUNT(*), COUNT(light)", //
		"D(F(M(C() P(E() 0)) 1 4 0 0))", //
		"P(F(C() 1 4 0 0) 0 0)");
		
		run("SELECT MIN(light), node GROUP BY node", //
		"D(F(M(C() P(E() 8 1)) 1 2 0 1 1))",//
		"P(F(C() 1 2 0 1 1) 0 1)");
		
		run("SELECT MIN(light), MAX(light) GROUP BY node", //
		"D(F(M(C() P(E() 8 8 1)) 2 2 0 3 1 1 2))", //
		"P(F(C() 2 2 0 3 1 1 2) 0 1)");
		
		run("SELECT COUNT(*) GROUP BY node", //
		"D(F(M(C() P(E() 0 1)) 1 4 0 1 1))", //
		"P(F(C() 1 4 0 1 1) 0)");
		run("SELECT COUNT(*), COUNT(light), MAX(temp), node GROUP BY node", //
		"D(F(M(C() P(E() 0 9 1)) 2 4 0 3 1 1 2))", //
		"P(F(C() 2 4 0 3 1 1 2) 0 0 1 2)");
		
		runParseError("SELECT COUNT()");
		runParseError("SELECT COUNT((node))");
		
		runCompileError("SELECT MIX(parent)");
		runCompileError("SELECT MIN(light), temp");
	}
	
	public void testWhere() throws QLParseException, QLCompileException {
		run("SELECT memory WHERE node < 12",// 
		"D(M(C() P(S(E() <(B(1) c(12))) 13 1)))", //
		"P(C() 0)");
		run("SELECT memory WHERE node <= 12", //
		"D(M(C() P(S(E() N(N(<(B(1) c(12)) <(B(1) c(12))) N(=(B(1) c(12)) =(B(1) c(12))))) 13 1)))", //
		"P(C() 0)");
		run("SELECT memory WHERE node == 12", //
		"D(M(C() P(S(E() =(B(1) c(12))) 13 1)))", //
		"P(C() 0)");
		run("SELECT memory WHERE node != 12", //
		"D(M(C() P(S(E() N(=(B(1) c(12)) =(B(1) c(12)))) 13 1)))", //
		"P(C() 0)");
		run("SELECT memory WHERE node >= 12", //
		"D(M(C() P(S(E() N(<(B(1) c(12)) <(B(1) c(12)))) 13 1)))", //
		"P(C() 0)");
		run("SELECT memory WHERE node > 12", //
		"D(M(C() P(S(E() N(N(N(<(B(1) c(12)) <(B(1) c(12))) N(=(B(1) c(12)) =(B(1) c(12)))) N(N(<(B(1) c(12)) <(B(1) c(12))) N(=(B(1) c(12)) =(B(1) c(12)))))) 13 1)))", //
		"P(C() 0)");
		
		run("SELECT temp, sw1 WHERE node == 12", //
		"D(M(C() P(S(E() =(B(1) c(12))) 9 6 1)))", //
		"P(C() 0 1)");
		run("SELECT temp, sw1 WHERE node == 0000.0000.0000.000C", //
		"D(M(C() P(S(E() =(B(1) e(12))) 9 6 1)))", //
		"P(C() 0 1)");
		
		run("SELECT x WHERE x == x AND y + 2 < 3", //
		"D(M(C() P(S(E() N(N(=(B(3) B(3)) <(+(B(4) c(2)) c(3))) N(=(B(3) B(3)) <(+(B(4) c(2)) c(3))))) 3 4)))", //
		"P(C() 0)");
		run("SELECT x WHERE x == x OR y + 2 < 3", //
		"D(M(C() P(S(E() N(N(=(B(3) B(3)) =(B(3) B(3))) N(<(+(B(4) c(2)) c(3)) <(+(B(4) c(2)) c(3))))) 3 4)))", //
		"P(C() 0)");
		run("SELECT x WHERE NOT x == x", //
		"D(M(C() P(S(E() N(=(B(3) B(3)) =(B(3) B(3)))) 3)))", //
		"P(C() 0)");
		
		run("SELECT memory WHERE memory == (12 + light)", //
		"D(M(C() P(S(E() =(B(13) +(c(12) B(8)))) 13 8)))", //
		"P(C() 0)");
		run("SELECT node WHERE 1 != (3 < 4)", //
		"D(M(C() P(S(E() N(=(c(1) <(c(3) c(4))) =(c(1) <(c(3) c(4))))) 1)))", //
		"P(C() 0)");
		run("SELECT node WHERE memory == (12 + light) AND 1 != (3 < 4)", //
		"D(M(C() P(S(E() N(N(=(B(13) +(c(12) B(8))) N(=(c(1) <(c(3) c(4))) =(c(1) <(c(3) c(4))))) N(=(B(13) +(c(12) B(8))) N(=(c(1) <(c(3) c(4))) =(c(1) <(c(3) c(4))))))) 1 13 8)))", //
		"P(C() 0)");
		
		runParseError("SELECT * WHERE light > 22.5f");
		runParseError("SELECT * WHERE node == \"hello world\"");
		runParseError("SELECT * WHERE node <> \"hello world\"");
	}
	
	public void testWhereAgg() throws QLCompileException, QLParseException {
		runParseError("SELECT * WHERE");
		
		runCompileError("SELECT * WHERE MAX(node) == 1");
		runCompileError("SELECT * WHERE MIN(node) == 1");
		runCompileError("SELECT * WHERE SUM(node) == 1");
		runCompileError("SELECT * WHERE AVG(node) == 1");
		runCompileError("SELECT * WHERE COUNT(node) == 1");
		runCompileError("SELECT * WHERE node >= 45 AND 1 == MIN(cpu)");
	}
	
	public void testGroupBy() throws QLParseException, QLCompileException {
		run("SELECT MAX(light) GROUP BY battery", //
		"D(F(M(C() P(E() 8 11)) 1 3 0 1 1))", //
		"P(F(C() 1 3 0 1 1) 0)");
		run("SELECT MAX(light), MAX(temp) GROUP BY battery", //
		"D(F(M(C() P(E() 8 9 11)) 2 3 0 3 1 1 2))", //
		"P(F(C() 2 3 0 3 1 1 2) 0 1)");
		run("SELECT MAX(light), MAX(temp)   GROUP BY parent, battery", //
		"D(F(M(C() P(E() 8 9 10 11)) 2 3 0 3 1 2 2 3))", //
		"P(F(C() 2 3 0 3 1 2 2 3) 0 1)");
		
		run("SELECT sw2, MAX(time) WHERE x == y + 24 * sw1 GROUP BY sw2", //
		"D(F(M(C() P(S(E() =(B(3) +(B(4) *(c(24) B(6))))) 7 2 3 4 6)) 1 3 1 1 0))", //
		"P(F(C() 1 3 1 1 0) 0 1)");
		run("SELECT MAX(time), light WHERE x == (y + 24) * sw1 GROUP BY sw2, light", //
		"D(F(M(C() P(S(E() =(B(3) *(+(B(4) c(24)) B(6)))) 2 8 3 4 6 7)) 1 3 0 2 5 1))", //
		"P(F(C() 1 3 0 2 5 1) 0 1)");
		
		runParseError("SELECT MIN(node) GROUP BY");
		
		runCompileError("SELECT MIN(*) GROUP BY node");
		runCompileError("SELECT * GROUP BY node");
		runCompileError("SELECT node GROUP BY node");
	}
	
	public void testHaving() throws QLParseException, QLCompileException {
		run("SELECT MAX(node) HAVING MAX(node) < 12", //
		"D(F(M(C() P(E() 1)) 1 3 0 0))", //
		"P(S(F(C() 1 3 0 0) <(B(0) c(12))) 0)");
		run("SELECT MAX(light), MAX(node) GROUP BY light HAVING MAX(node) < 12", //
		"D(F(M(C() P(E() 8 1 8)) 2 3 0 3 1 1 2))", //
		"P(S(F(C() 2 3 0 3 1 1 2) <(B(1) c(12))) 0 1)");
		run("SELECT node, MIN(parent), MAX(time) WHERE x == (y + 24) * sw1 GROUP BY node HAVING MAX(time) < 1234567", //
		"D(F(M(C() P(S(E() =(B(3) *(+(B(4) c(24)) B(6)))) 1 10 2 3 4 6)) 2 2 1 3 2 1 0))", //
		"P(S(F(C() 2 2 1 3 2 1 0) <(B(2) c(1234567))) 0 1 2)");
		run("SELECT parent, COUNT(*) GROUP BY parent HAVING COUNT(*) > 5", //
		"D(F(M(C() P(E() 0 10)) 1 4 0 1 1))", //
		"P(S(F(C() 1 4 0 1 1) N(N(N(<(B(0) c(5)) <(B(0) c(5))) N(=(B(0) c(5)) =(B(0) c(5)))) N(N(<(B(0) c(5)) <(B(0) c(5))) N(=(B(0) c(5)) =(B(0) c(5)))))) 1 0)");
		run("SELECT parent GROUP BY parent HAVING COUNT(*) > 5", //
		"D(F(M(C() P(E() 0 10)) 1 4 0 1 1))", //
		"P(S(F(C() 1 4 0 1 1) N(N(N(<(B(0) c(5)) <(B(0) c(5))) N(=(B(0) c(5)) =(B(0) c(5)))) N(N(<(B(0) c(5)) <(B(0) c(5))) N(=(B(0) c(5)) =(B(0) c(5)))))) 1)");
		
		runCompileError("SELECT * HAVING node < 12");
	}
}
