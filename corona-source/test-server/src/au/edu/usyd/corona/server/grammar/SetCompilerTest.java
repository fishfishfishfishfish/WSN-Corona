package au.edu.usyd.corona.server.grammar;


import au.edu.usyd.corona.server.user.User;

public class SetCompilerTest extends CompilerTestCase {
	
	private void run(String query, User user, boolean shouldPass) {
		try {
			QLCompiler.getInstance().compile("SET " + query, 0, user);
		}
		catch (QLParseException e) {
			fail(e.getMessage());
		}
		catch (QLCompileException e) {
			if (shouldPass)
				fail(e.getMessage());
			return;
		}
		if (!shouldPass)
			fail("Did not die");
	}
	
	public void testValidProperties() {
		run("sync_epoch = 1234", adminUser, true);
		run("sync_epoch = 1234567890", adminUser, true);
		run("sync_epoch    = 5674", adminUser, true);
		run("monitoring_epoCH = 24564", adminUser, true);
		run("reroute_EPOCH = 987654321", adminUser, true);
		
		run("monitdoring_period = 24564", adminUser, false);
		run("x = 24564", adminUser, false);
		run("monitoring_period = 24564", adminUser, false);
		
		run("intercluster_power = 2", adminUser, true);
		run("intracluster_power = 2", adminUser, true);
		run("intercluster_power = 31", adminUser, true);
		run("intercluster_power = -32", adminUser, true);
		
		run("intercluster_power = -33", adminUser, false);
		run("intercluster_power = 32", adminUser, false);
	}
	
	public void testValidValues() {
		run("sync_epoch = 7896", adminUser, true);
		run("sync_epoch = 1234567890", adminUser, true);
		run("sync_epoch = 1000", adminUser, true);
		run("sync_epoch = -1000", adminUser, false);
		run("sync_epoch = 999", adminUser, false);
		run("sync_epoch = 0", adminUser, false);
	}
	
	public void testDifferentUsers() {
		run("sync_epoch = 789465", adminUser, true);
		run("sync_epoch = 456789", normalUser, false);
	}
	
}
