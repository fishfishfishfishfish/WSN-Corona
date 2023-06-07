package au.edu.usyd.corona.server.grammar;


import au.edu.usyd.corona.scheduler.KillTask;
import au.edu.usyd.corona.scheduler.SchedulableTask;

public class KillCompilerTest extends CompilerTestCase {
	
	private void _run(int taskId) throws QLParseException, QLCompileException {
		SchedulableTask q = QLCompiler.getInstance().compile("KILL " + taskId, 0, adminUser);
		assertEquals(taskId, ((KillTask) q).getKillID());
	}
	
	private void runParseError(String query) throws QLCompileException {
		try {
			QLCompiler.getInstance().compile(query, 0, adminUser);
			fail("QLParseException should have been thrown");
		}
		catch (QLParseException e) {
		}
	}
	
	private void runCompileError(String query) throws QLParseException {
		try {
			QLCompiler.getInstance().compile(query, 0, adminUser);
			fail("QLCompileException should have been thrown");
		}
		catch (QLCompileException e) {
		}
	}
	
	public void test1() throws QLParseException, QLCompileException {
		_run(0);
		_run(Integer.MAX_VALUE);
		_run(Integer.MIN_VALUE);
		
		runParseError("KILL");
		runParseError("KILL fish");
		runParseError("KILL \"1\"");
		
		runCompileError("KILL \t" + Long.MAX_VALUE);
	}
}
