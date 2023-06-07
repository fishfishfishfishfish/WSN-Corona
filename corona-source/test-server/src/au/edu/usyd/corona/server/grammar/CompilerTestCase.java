package au.edu.usyd.corona.server.grammar;


import junit.framework.TestCase;
import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.server.user.User;
import au.edu.usyd.corona.server.user.User.AccessLevel;

abstract class CompilerTestCase extends TestCase {
	protected User adminUser;
	protected User normalUser;
	
	@Override
	public void setUp() {
		Network.initialize(Network.MODE_UNITTEST);
		adminUser = new User("a", "a", AccessLevel.ADMIN);
		normalUser = new User("b", "b", AccessLevel.USER);
	}
}
