package au.edu.usyd.corona.server.grammar;


public class QueryCompilerGrammarTest extends CompilerTestCase {
	
	private void expectFail(String query) {
		try {
			QLCompiler.getInstance().compile(query, 0, adminUser);
		}
		catch (QLCompileException e) {
			return;
		}
		catch (QLParseException e) {
			return;
		}
		fail("Expected QLCompileException for '" + query + "'; nothing was thrown");
	}
	
	private void expectPass(String query) throws Exception {
		QLCompiler.getInstance().compile(query, 0, adminUser);
	}
	
	public void test1() throws Exception {
		expectPass("SELECT MAX(battery), light GROUP BY light HAVING light > 12");
		expectPass("SELECT MAX(battery) GROUP BY light HAVING light > 12");
		
		expectFail("SELECT MAX(battery) WHERE MAX(battery) < 12");
		
		expectPass("SELECT * RUNCOUNT 20");
		expectPass("SELECT * RUNCOUNT FOREVER");
		expectPass("SELECT * RUNCOUNT " + Integer.MAX_VALUE);
		expectFail("SELECT * RUNCOUNT 0");
		expectFail("SELECT * RUNCOUNT -20");
		expectFail("SELECT * RUNCOUNT " + (((long) Integer.MAX_VALUE) + 1));
		
		expectPass("SELECT * FROM SENSORS");
		expectFail("SELECT * FROM");
		expectFail("SELECT * FROM FISH");
		expectFail("SELECT * FROM NETWORK");
	}
}
