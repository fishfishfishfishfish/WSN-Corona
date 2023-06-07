package au.edu.usyd.corona.server.persistence.JDBCDAO;


import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;
import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteUserResultsInterface;
import au.edu.usyd.corona.server.persistence.DAOinterface.UserDAO;
import au.edu.usyd.corona.server.session.UserRetrieveException;
import au.edu.usyd.corona.server.user.User;
import au.edu.usyd.corona.server.user.User.AccessLevel;

/**
 * @author Edmund Tse
 */
public class JDBCUserDAOTest extends TestCase {
	JDBCDAOFactory factory;
	JDBCUserDAO ud;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		factory = new JDBCDAOFactory();
		ud = (JDBCUserDAO) factory.getUserDAO();
	}
	
	@Override
	protected void tearDown() throws Exception {
		factory.clean();
		factory.close();
		
		super.tearDown();
	}
	
	/**
	 * Test method for
	 * {@link au.edu.usyd.corona.server.persistence.JDBCDAO.JDBCUserDAO#init()} .
	 * 
	 * @throws DAOException
	 * @throws RemoteException
	 * @throws UserRetrieveException
	 */
	public void testInitialise() throws DAOException, RemoteException, UserRetrieveException {
		// Expect query DAO to be already initialised when getQueryDAO() is called.
		// But it should be safe to call initialise again without causing an exception.
		ud.init();
		
		// Test default user
		assertTrue(ud.userExists(UserDAO.DEFAULT_ADMIN));
		
		// Test ability for initialise to find a free username
		ud.updateUser(UserDAO.DEFAULT_ADMIN, null, AccessLevel.USER);
		ud.init();
		
		for (int i = 0; i < 50; i++) {
			// Change all admins to users
			RemoteUserResultsInterface adminResults = ud.retrieveUsers("access_level = '" + AccessLevel.ADMIN + "'", "");
			List<User> admins = adminResults.getItems(0, adminResults.getNumItems());
			for (User u : admins)
				ud.updateUser(u.getId(), null, null, AccessLevel.USER);
			
			// Try to find a free username to make admin
			ud.init();
		}
	}
	
	/**
	 * Test method for
	 * {@link au.edu.usyd.corona.server.persistence.JDBCDAO.JDBCUserDAO#userExists(java.lang.String)}
	 * .
	 * 
	 * @throws DAOException
	 */
	public void testUserExists() throws DAOException {
		// Zero: Null parameter throws IllegalArgumentException
		try {
			assertFalse(ud.userExists(null));
			fail("IllegalArgumentException not thrown");
		}
		catch (IllegalArgumentException e) {
		}
		
		// Normal: When requested user doesn't exist
		assertFalse(ud.userExists("user"));
		
		// Normal: When requested user does exist
		ud.addUser("user", "");
		assertTrue(ud.userExists("user"));
		
		// SQL wildcards
		assertFalse(ud.userExists("%"));
		assertFalse(ud.userExists("*"));
		
		// Upper and lower cases - expect to be case insensitive
		assertTrue(ud.userExists("User"));
		assertTrue(ud.userExists("USER"));
		
		// Surrounding white spaces - should be ignored
		assertTrue(ud.userExists(" user "));
		assertTrue(ud.userExists("\tuser\t"));
		assertTrue(ud.userExists("\nuser\n"));
		
		// White spaces between words - should not be ignored
		assertFalse(ud.userExists("us er"));
		assertFalse(ud.userExists("us\ter"));
		assertFalse(ud.userExists("us\ner"));
		ud.addUser("us\ter", "");
		assertFalse(ud.userExists("us er"));
		assertTrue(ud.userExists("us\ter"));
		assertFalse(ud.userExists("us\ner"));
	}
	
	/**
	 * Test method for
	 * {@link au.edu.usyd.corona.server.persistence.JDBCDAO.JDBCUserDAO#retrieveUsers()}
	 * .
	 * 
	 * @throws DAOException
	 * @throws RemoteException
	 * @throws UserRetrieveException
	 */
	public void testgetItems() throws DAOException, RemoteException, UserRetrieveException {
		List<User> result;
		RemoteUserResultsInterface resultsProxy;
		
		// Fresh database; should have only one user (default user)
		resultsProxy = ud.retrieveUsers();
		result = resultsProxy.getItems(0, resultsProxy.getNumItems());
		assertEquals(1, result.size());
		assertTrue(containsUsername(result, UserDAO.DEFAULT_ADMIN));
		
		// Normal: after adding one user
		ud.addUser("user", "");
		resultsProxy = ud.retrieveUsers();
		result = resultsProxy.getItems(0, resultsProxy.getNumItems());
		assertEquals(2, result.size());
		assertTrue(containsUsername(result, "user"));
		
		// Normal: after adding more than one user
		ud.addUser("user2", "", AccessLevel.ADMIN);
		resultsProxy = ud.retrieveUsers();
		result = resultsProxy.getItems(0, resultsProxy.getNumItems());
		assertEquals(3, result.size());
		assertTrue(containsUsername(result, "user"));
		assertTrue(containsUsername(result, "user2"));
		
		// Getting users subject to a certain condition
		// Zero: Null parameter means no condition specified
		resultsProxy = ud.retrieveUsers(null, null);
		result = resultsProxy.getItems(0, resultsProxy.getNumItems());
		assertEquals(3, result.size());
		assertTrue(containsUsername(result, UserDAO.DEFAULT_ADMIN));
		assertTrue(containsUsername(result, "user"));
		assertTrue(containsUsername(result, "user2"));
		
		// Zero: Empty string also means no condition specified
		resultsProxy = ud.retrieveUsers("", "");
		result = resultsProxy.getItems(0, resultsProxy.getNumItems());
		assertEquals(3, result.size());
		assertTrue(containsUsername(result, UserDAO.DEFAULT_ADMIN));
		assertTrue(containsUsername(result, "user"));
		assertTrue(containsUsername(result, "user2"));
		
		// Normal: filtering based on known fields
		resultsProxy = ud.retrieveUsers("username = 'user'", null);
		result = resultsProxy.getItems(0, resultsProxy.getNumItems());
		assertEquals(1, result.size());
		assertTrue(containsUsername(result, "user"));
		
		resultsProxy = ud.retrieveUsers("access_level = 'ADMIN'", null);
		result = resultsProxy.getItems(0, resultsProxy.getNumItems());
		assertEquals(2, result.size());
		assertTrue(containsUsername(result, UserDAO.DEFAULT_ADMIN));
		assertTrue(containsUsername(result, "user2"));
		
		// Error: Unknown fields
		try {
			ud.retrieveUsers("abcde", null).getNumItems();
			fail("Did not throw exception when using unknown fields");
		}
		catch (UserRetrieveException e) {
		}
		try {
			ud.retrieveUsers("abc = abc", null).getNumItems();
			fail("Did not throw exception when using unknown fields");
		}
		catch (UserRetrieveException e) {
		}
		
		// Error: Bad where expression
		try {
			ud.retrieveUsers("ids = 'a'", null).getNumItems();
			fail("Did not throw exception when using unknown fields");
		}
		catch (UserRetrieveException e) {
		}
		
		// Error: piggybacked where expression should not be executed
		try {
			ud.retrieveUsers("true; DROP TABLE abcde;", null).getNumItems();
		}
		catch (DAOException e) {
			fail("Executed piggybacked SQL statement");
			// Because table "abcde" does not exist, this throws Exception if executed.
		}
	}
	
	private boolean containsUsername(Collection<User> users, String username) {
		for (User u : users)
			if (u.getUsername().equals(username))
				return true;
		return false;
	}
	
	/**
	 * Test method for
	 * {@link au.edu.usyd.corona.server.persistence.JDBCDAO.JDBCUserDAO#addUser(java.lang.String, java.lang.String)}
	 * .
	 * 
	 * @throws DAOException
	 */
	public void testAddUser() throws DAOException {
		// Zero: Any null parameters should throw exception
		try {
			ud.addUser("user", "pass", null);
			fail("Exception not thrown when adding user with null access level");
		}
		catch (IllegalArgumentException e) {
		}
		
		try {
			ud.addUser("user", null, AccessLevel.USER);
			fail("Exception not thrown when adding user with null password");
		}
		catch (IllegalArgumentException e) {
		}
		
		try {
			ud.addUser(null, "pass", AccessLevel.USER);
			fail("Exception not thrown when adding user with null username");
		}
		catch (IllegalArgumentException e) {
		}
		
		// Zero: when there are no users in the database
		ud.addUser("user0", "");
		assertTrue(ud.userExists("user0"));
		
		// Error: duplicate entries are not allowed
		try {
			ud.addUser("user0", "");
			fail("Did not throw DAOException when attempting to re-add an existing user");
		}
		catch (DAOException e) {
		}
		
		// Property: should ignore surrounding white spaces
		ud.addUser(" user1 ", "");
		assertTrue(ud.userExists("user1"));
		ud.addUser("\tuser2\t", "");
		assertTrue(ud.userExists("user2"));
		ud.addUser("\nuser3\n", "");
		assertTrue(ud.userExists("user3"));
		
		// Property: should be case insensitive
		ud.addUser("User4", "");
		assertTrue(ud.userExists("user4"));
		ud.addUser("USER5", "");
		assertTrue(ud.userExists("user5"));
		
		// Property: internal white spaces are significant
		ud.addUser("user 6", "");
		assertTrue(ud.userExists("user 6"));
		ud.addUser(" u s e r 7 ", "");
		assertTrue(ud.userExists("u s e r 7"));
		ud.addUser("\nus\t\ner8\t", "");
		assertTrue(ud.userExists("us\t\ner8"));
	}
	
	/**
	 * Test method for
	 * {@link au.edu.usyd.corona.server.persistence.JDBCDAO.JDBCUserDAO#updateUser(String, String, AccessLevel)}
	 * 
	 * @throws DAOException
	 * @throws RemoteException
	 */
	public void testUpdateUser() throws DAOException, RemoteException {
		// Zero: Any null parameters should throw exception
		try {
			ud.updateUser(null, "pass", AccessLevel.USER);
			fail("Exception not thrown when updating user with null username");
		}
		catch (IllegalArgumentException e) {
		}
		
		// Zero: nonexistent user
		try {
			ud.updateUser("user0", "pass0", AccessLevel.USER);
			fail();
		}
		catch (DAOException e) {
		}
		
		User u;
		
		// Normal: existing user
		User u1 = new User("user1", "pass1", AccessLevel.ADMIN);
		ud.addUser(u1);
		u1.setAccessLevel(AccessLevel.USER);
		try {
			ud.updateUser(u1.getUsername(), u1.getPassword(), u1.getAccessLevel());
		}
		catch (DAOException e) {
			e.printStackTrace();
			fail();
		}
		u = ud.checkPassword(u1.getUsername(), u1.getPassword());
		assertNotNull(u);
		assertEquals(AccessLevel.USER, u.getAccessLevel());
		
		// Zero: choose not to update any details - of course would be successful
		try {
			ud.updateUser("user1", null, null);
		}
		catch (DAOException e) {
			fail();
		}
		try {
			ud.updateUser(1, null, null, null);
		}
		catch (DAOException e) {
			fail();
		}
		
		// Normal: Should only affect one user
		User u2 = new User("user2", "pass2", AccessLevel.USER);
		ud.addUser(u2);
		u2.setAccessLevel(AccessLevel.ADMIN);
		try {
			ud.updateUser(u2.getUsername(), null, u2.getAccessLevel());
		}
		catch (DAOException e) {
			fail();
		}
		u = ud.checkPassword(u2.getUsername(), u2.getPassword());
		assertNotNull(u);
		assertEquals(AccessLevel.ADMIN, u.getAccessLevel());
		
		u = ud.checkPassword(u1.getUsername(), u1.getPassword());
		assertNotNull(u);
		assertEquals(AccessLevel.USER, u.getAccessLevel());
		
		u2.setId(ud.checkPassword(u2.getUsername(), u2.getPassword()).getId());
		u2.setAccessLevel(AccessLevel.USER);
		try {
			ud.updateUser(u2.getId(), null, null, u2.getAccessLevel());
		}
		catch (DAOException e) {
			fail();
		}
		u = ud.checkPassword(u2.getUsername(), u2.getPassword());
		assertNotNull(u);
		assertEquals(AccessLevel.USER, u.getAccessLevel());
		
		// Normal: Choose not to update access level
		u2.setPassword("password2");
		try {
			
			ud.updateUser(u2.getUsername(), u2.getPassword(), null);
		}
		catch (DAOException e) {
			fail();
		}
		u = ud.checkPassword(u2.getUsername(), u2.getPassword());
		assertNotNull(u);
		
		u2.setPassword("p2");
		try {
			ud.updateUser(u2.getId(), null, u2.getPassword(), null);
		}
		catch (DAOException e) {
			fail();
		}
		u = ud.checkPassword(u2.getUsername(), u2.getPassword());
		assertNotNull(u);
		
		// Normal: Updating username
		u2 = new User(u2.getId(), "username2", u2.getPassword(), u2.getAccessLevel());
		try {
			ud.updateUser(u2.getId(), u2.getUsername(), null, null);
		}
		catch (DAOException e) {
			fail();
		}
		u = ud.checkPassword(u2.getUsername(), u2.getPassword());
		assertNotNull(u);
		assertEquals(u2.getUsername(), u.getUsername());
		
		// Normal: Choose not to update password
		u2.setAccessLevel(AccessLevel.USER);
		try {
			ud.updateUser(u2.getUsername(), null, u2.getAccessLevel());
		}
		catch (DAOException e) {
			fail();
		}
		u = ud.checkPassword(u2.getUsername(), u2.getPassword());
		assertNotNull(u);
		assertEquals(AccessLevel.USER, u.getAccessLevel());
		
		// Property: should ignore surrounding white spaces in username
		ud.updateUser(" user1 ", "", AccessLevel.USER);
		u = ud.checkPassword("user1", "");
		assertNotNull(u);
		assertEquals("user1", u.getUsername());
		assertEquals(AccessLevel.USER, u.getAccessLevel());
		
		ud.updateUser("\tuser1\t", "", AccessLevel.ADMIN);
		u = ud.checkPassword("user1", "");
		assertNotNull(u);
		assertEquals("user1", u.getUsername());
		assertEquals(AccessLevel.ADMIN, u.getAccessLevel());
		
		ud.updateUser("\nuser1\n", "", AccessLevel.USER);
		u = ud.checkPassword("user1", "");
		assertNotNull(u);
		assertEquals("user1", u.getUsername());
		assertEquals(AccessLevel.USER, u.getAccessLevel());
		
		// Property: should be case insensitive in username
		ud.updateUser("User1", "", AccessLevel.ADMIN);
		u = ud.checkPassword("user1", "");
		assertNotNull(u);
		assertEquals("user1", u.getUsername());
		assertEquals(AccessLevel.ADMIN, u.getAccessLevel());
		
		ud.updateUser("USER1", "", AccessLevel.USER);
		u = ud.checkPassword("user1", "");
		assertNotNull(u);
		assertEquals("user1", u.getUsername());
		assertEquals(AccessLevel.USER, u.getAccessLevel());
		
		// Property: internal white spaces are significant in username
		try {
			ud.updateUser("user 1", "", AccessLevel.ADMIN);
			fail();
		}
		catch (DAOException e) {
		}
	}
	
	/**
	 * Test method for
	 * {@link au.edu.usyd.corona.server.persistence.JDBCDAO.JDBCUserDAO#deleteUser(java.lang.String)}
	 * .
	 * 
	 * @throws DAOException
	 * @throws RemoteException
	 */
	public void testDeleteUser() throws DAOException, RemoteException {
		// Zero: null user
		try {
			ud.deleteUser(null);
			fail("Exception not thrown when deleting nonexistent user");
		}
		catch (IllegalArgumentException e) {
			// Expects this Exception be thrown
		}
		
		// Zero: empty username ( = nonexistent yet)
		ud.deleteUser("");
		
		// Zero: trying to delete a nonexistent user; postconditions still hold, so don't throw exception
		ud.deleteUser("user");
		
		// Normal: deleting one existing user
		assertFalse(ud.userExists("user"));
		ud.addUser("user", "");
		assertTrue(ud.userExists("user"));
		ud.deleteUser("user");
		assertFalse(ud.userExists("user"));
		
		// Normal: deleting one of many existing users
		ud.addUser("user1", "");
		ud.addUser("user2", "");
		ud.addUser("user3", "");
		assertTrue(ud.userExists("user2"));
		ud.deleteUser("user2");
		assertFalse(ud.userExists("user2"));
		
		// Property: should be case insensitive
		assertFalse(ud.userExists("user10"));
		ud.addUser("user10", "");
		assertTrue(ud.userExists("user10"));
		ud.deleteUser("User10");
		assertFalse(ud.userExists("user10"));
		
		assertFalse(ud.userExists("user11"));
		ud.addUser("user11", "");
		assertTrue(ud.userExists("user11"));
		ud.deleteUser("USER11");
		assertFalse(ud.userExists("user11"));
		
		// Property: should ignore surrounding white spaces
		assertFalse(ud.userExists("user12"));
		ud.addUser("user12", "");
		assertTrue(ud.userExists("user12"));
		ud.deleteUser(" user12 ");
		assertFalse(ud.userExists("user12"));
		
		assertFalse(ud.userExists("user12"));
		ud.addUser("user12", "");
		assertTrue(ud.userExists("user12"));
		ud.deleteUser("\tuser12\t");
		assertFalse(ud.userExists("user12"));
		
		assertFalse(ud.userExists("user12"));
		ud.addUser("user12", "");
		assertTrue(ud.userExists("user12"));
		ud.deleteUser("\nuser12\n");
		assertFalse(ud.userExists("user12"));
		
		// Property: internal white spaces should be significant
		assertFalse(ud.userExists("us  \n\ter20"));
		ud.addUser("us  \n\ter20", "");
		assertTrue(ud.userExists("us  \n\ter20"));
		ud.deleteUser("us  \n\ter20");
		assertFalse(ud.userExists("us  \n\ter20"));
		
		assertFalse(ud.userExists("user20"));
		ud.addUser("user20", "");
		assertTrue(ud.userExists("user20"));
		ud.deleteUser("us  \t\ner20");
		assertTrue(ud.userExists("user20"));
		ud.deleteUser("user20");
		assertFalse(ud.userExists("user20"));
	}
	
	/**
	 * Test method for
	 * {@link au.edu.usyd.corona.server.persistence.JDBCDAO.JDBCUserDAO#checkPassword(java.lang.String, java.lang.String)}
	 * .
	 * 
	 * @throws DAOException
	 */
	public void testCheckPassword() throws DAOException {
		// Zero: Check password for null user
		try {
			ud.checkPassword(null, "pass");
			fail("Exception not thrown while checking null user");
		}
		catch (IllegalArgumentException e) {
		}
		
		try {
			ud.checkPassword("user", null);
			fail("Exception not thrown while checking null user");
		}
		catch (IllegalArgumentException e) {
		}
		
		// Zero: check password for nonexistent user - obviously incorrect
		assertNull(ud.checkPassword("user", ""));
		
		// Property: empty password can be denoted by empty string or null
		ud.addUser("user", "");
		assertNotNull(ud.checkPassword("user", ""));
		ud.addUser("user1", "");
		assertNotNull(ud.checkPassword("user1", ""));
		
		// Property: username should be case insensitive
		assertNotNull(ud.checkPassword("User", ""));
		assertNotNull(ud.checkPassword("USER", ""));
		
		// Property: password should be case sensitive
		ud.addUser("user2", "pass");
		assertNotNull(ud.checkPassword("user2", "pass"));
		assertNull(ud.checkPassword("user2", "Pass"));
		assertNull(ud.checkPassword("user2", "PASS"));
		
		// Property: all spaces in password string are significant
		assertNull(ud.checkPassword("user2", " pass "));
		assertNull(ud.checkPassword("user2", "pa ss"));
		ud.addUser("user3", " pass ");
		assertNotNull(ud.checkPassword("user3", " pass "));
		assertNull(ud.checkPassword("user3", " pa ss "));
		
		// SQL wildcards
		assertNull(ud.checkPassword("user", "%"));
		assertNull(ud.checkPassword("user", "*"));
	}
}
