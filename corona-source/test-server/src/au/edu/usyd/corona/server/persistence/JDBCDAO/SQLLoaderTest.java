package au.edu.usyd.corona.server.persistence.JDBCDAO;


import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class SQLLoaderTest extends TestCase {
	public void testCase1() throws Exception {
		SQLLoader sql = new SQLLoader("testfiles/test1.xml", "hsqldb");
		Map<String, String> namedParams = new HashMap<String, String>();
		namedParams.put("{accessLevel}", "test1");
		namedParams.put("{password}", "test2");
		namedParams.put("{username}", "test3");
		assertEquals("UPDATE users SET access_levels = test1 password = test2 WHERE username = test3".trim(), sql.getSQLString("TEST_1", namedParams).trim());
	}
}
