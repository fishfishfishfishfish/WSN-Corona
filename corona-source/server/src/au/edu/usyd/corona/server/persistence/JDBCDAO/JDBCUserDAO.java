package au.edu.usyd.corona.server.persistence.JDBCDAO;


import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import au.edu.usyd.corona.server.grammar.Query;
import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;
import au.edu.usyd.corona.server.persistence.DAOinterface.DAOFactory;
import au.edu.usyd.corona.server.persistence.DAOinterface.QueryDAO;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteQueryResultsInterface;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteUserResultsInterface;
import au.edu.usyd.corona.server.persistence.DAOinterface.UserDAO;
import au.edu.usyd.corona.server.session.QueryRetrieveException;
import au.edu.usyd.corona.server.session.UserRetrieveException;
import au.edu.usyd.corona.server.user.User;
import au.edu.usyd.corona.server.user.User.AccessLevel;
import au.edu.usyd.corona.server.util.Hasher;
import au.edu.usyd.corona.server.util.SQLExtractor;

/**
 * A specific implementation of the UserDAO for JDBC databases.
 * 
 * @author Raymes Khoury
 */
class JDBCUserDAO implements UserDAO {
	public static final String TABLE_NAME = "users";
	
	public static final String NAMED_PARAM_WHERE = "{where}";
	public static final String NAMED_PARAM_START = "{start}";
	public static final String NAMED_PARAM_NUM = "{num}";
	
	protected static final String CREATE_USERS_TABLE_KEY = "CREATE_USERS_TABLE";
	protected static final String INSERT_USER_KEY = "INSERT_USER";
	protected static final String UPDATE_USER_USERNAME_KEY = "UPDATE_USER_USERNAME";
	protected static final String UPDATE_USER_ACCESS_KEY = "UPDATE_USER_ACCESS";
	protected static final String UPDATE_USER_PASS_KEY = "UPDATE_USER_PASSWORD";
	protected static final String GET_USER_KEY = "GET_USER";
	protected static final String DELETE_USER_KEY = "DELETE_USER";
	protected static final String CHECK_PASSWORD_KEY = "CHECK_USER_PASSWORD";
	
	private static final Logger logger = Logger.getLogger(JDBCUserDAO.class.getCanonicalName());
	
	private final DataSource dataSource;
	private final SQLLoader sqlStatements;
	
	protected JDBCUserDAO(DataSource dataSource, SQLLoader sqlStatements) throws DAOException {
		this.dataSource = dataSource;
		this.sqlStatements = sqlStatements;
	}
	
	/**
	 * Prepares the database for storage of user details. Creates the table that
	 * would be used to store user information, if necessary. Also checks if
	 * there is at least one administrator in the system. If no administrators
	 * exist, it tries to create an administrator using the default administrator
	 * username and password specified in UserDAO.
	 * 
	 * @throws RemoteException
	 */
	protected void init() throws DAOException, RemoteException {
		// Create the users table if it does not already exist
		Connection conn = null;
		PreparedStatement ps = null;
		
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			if (DBUtils.tableExists(TABLE_NAME, conn, sqlStatements)) {
				return;
			}
			ps = sqlStatements.buildSQLStatement(conn, CREATE_USERS_TABLE_KEY);
			ps.executeUpdate();
			conn.commit();
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Cannot create User table", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Cannot create User table", e);
		}
		finally {
			DBUtils.closeStatement(ps);
			DBUtils.closeConnection(conn);
		}
		
		// Add an administrator account if none exists
		RemoteUserResultsInterface userResults = retrieveUsers();
		Collection<User> users = null;
		try {
			users = userResults.getItems(0, userResults.getNumItems());
		}
		catch (UserRetrieveException e) {
			throw new DAOException("Failed to get users: " + e);
		}
		for (User u : users)
			if (u.getAccessLevel() == AccessLevel.ADMIN)
				return; // There is an administrator. End initialisation.
				
		// Try to create a default administrator
		String username = DEFAULT_ADMIN;
		if (userExists(username)) { // Default administrator username is in use. Find a free username
			for (int i = 0;; i++)
				if (!userExists(username + i)) { // This username is free
					username = username + i;
					break;
				}
		}
		
		try {
			addUser(username, Hasher.hash(DEFAULT_ADMIN_PASSWORD), AccessLevel.ADMIN);
			logger.warning("No administrators found - default administrator created: " + username);
		}
		catch (DAOException e) {
			logger.warning("Could not create default administrator: " + e.getMessage());
		}
		
	}
	
	public boolean userExists(String username) throws DAOException {
		if (username == null)
			throw new IllegalArgumentException("Null username");
		
		username = username.trim().toLowerCase();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			
			Map<String, String> namedParams = new HashMap<String, String>();
			String where = new SQLExtractor("username = '" + username + "'", SQLExtractor.Type.WHERE).extractWhere();
			namedParams.put(NAMED_PARAM_WHERE, where);
			
			ps = sqlStatements.buildSQLStatement(conn, GET_USER_KEY, namedParams);
			rs = ps.executeQuery();
			
			if (rs.next() && rs.getString(2).equalsIgnoreCase(username))
				return true;
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Cannot get Usernames", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Cannot get Usernames", e);
		}
		finally {
			DBUtils.closeResultSet(rs);
			DBUtils.closeStatement(ps);
			DBUtils.closeConnection(conn);
		}
		
		return false;
	}
	
	public RemoteUserResultsInterface retrieveUsers() throws DAOException, RemoteException {
		logger.fine("Getting all users");
		return retrieveUsers(null, null);
	}
	
	public RemoteUserResultsInterface retrieveUsers(String whereClause, String orderByClause) throws DAOException, RemoteException {
		return new RemoteJDBCUserResults(sqlStatements, dataSource, whereClause, orderByClause);
	}
	
	public void addUser(User user) throws DAOException {
		if (user == null)
			throw new IllegalArgumentException("Null user");
		addUser(user.getUsername(), user.getPassword(), user.getAccessLevel());
	}
	
	public void addUser(String username, String passwordHash) throws DAOException {
		addUser(username, passwordHash, User.DEFAULT_ACCESS_LEVEL);
	}
	
	public synchronized void addUser(String username, String passwordHash, AccessLevel accessLevel) throws DAOException {
		if (username == null)
			throw new IllegalArgumentException("Invalid username (null)");
		else if (passwordHash == null)
			throw new IllegalArgumentException("Invalid password (null)");
		else if (accessLevel == null)
			throw new IllegalArgumentException("Invalid access level (null)");
		
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			// Make case insensitive
			Object[] parameters = {username.trim().toLowerCase(), passwordHash, accessLevel.toString()};
			ps = sqlStatements.buildSQLStatement(conn, INSERT_USER_KEY, parameters);
			ps.executeUpdate();
			conn.commit();
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Cannot add User", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Cannot add User", e);
		}
		finally {
			DBUtils.closeStatement(ps);
			DBUtils.closeConnection(conn);
		}
	}
	
	public synchronized void updateUser(String username, String passwordHash, AccessLevel accessLevel) throws DAOException {
		if (username == null)
			throw new IllegalArgumentException("Null username");
		
		String user = username.trim().toLowerCase();
		
		Collection<User> users = null;
		try {
			RemoteUserResultsInterface userResults = retrieveUsers("username = " + "'" + user.trim().toLowerCase() + "'", "");
			users = userResults.getItems(0, userResults.getNumItems());
		}
		catch (UserRetrieveException e) {
			throw new DAOException("Failed to get users: " + e);
		}
		catch (RemoteException e) {
			throw new DAOException("Failed to get users: " + e);
		}
		
		if (users.size() > 1) {
			logger.severe("Found more than one user with username: " + user);
			throw new DAOException("Found more than one user with username: " + user);
		}
		
		for (User u : users)
			updateUser(u.getId(), null, passwordHash, accessLevel);
		
		if (users.size() == 0)
			throw new DAOException("Failed to update user");
	}
	
	public synchronized void updateUser(int userId, String username, String passwordHash, AccessLevel accessLevel) throws DAOException {
		Connection conn = null;
		PreparedStatement ps = null;
		
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			int updated = 0;
			if (username != null) {
				logger.fine("Updating user (" + userId + ") username");
				updated--;
				Object[] parameters = {username.trim().toLowerCase(), userId};
				ps = sqlStatements.buildSQLStatement(conn, UPDATE_USER_USERNAME_KEY, parameters);
				updated += ps.executeUpdate();
			}
			if (passwordHash != null) {
				logger.fine("Updating user (" + userId + ") password");
				updated--;
				Object[] parameters = {passwordHash, userId};
				ps = sqlStatements.buildSQLStatement(conn, UPDATE_USER_PASS_KEY, parameters);
				updated += ps.executeUpdate();
			}
			if (accessLevel != null) {
				logger.fine("Updating user (" + userId + ") access level");
				updated--;
				Object[] parameters = {accessLevel.toString(), userId};
				ps = sqlStatements.buildSQLStatement(conn, UPDATE_USER_ACCESS_KEY, parameters);
				updated += ps.executeUpdate();
			}
			
			conn.commit();
			
			if (updated == 0)
				return;
			
			DBUtils.rollbackConn(conn); // Don't allow partial update to the user
			throw new DAOException("Failed to update user");
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Cannot add User", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Cannot add User", e);
		}
		finally {
			DBUtils.closeStatement(ps);
			DBUtils.closeConnection(conn);
		}
	}
	
	public synchronized void deleteUser(String username) throws DAOException {
		if (username == null)
			throw new IllegalArgumentException("Null username");
		
		String user = username.trim().toLowerCase();
		
		Collection<User> users = null;
		RemoteUserResultsInterface userResults = null;
		try {
			userResults = retrieveUsers("username = " + "'" + user.trim().toLowerCase() + "'", "");
			users = userResults.getItems(0, userResults.getNumItems());
		}
		catch (UserRetrieveException e) {
			throw new DAOException("Failed to get users: " + e);
		}
		catch (RemoteException e) {
			throw new DAOException("Failed to get users: " + e);
		}
		if (users.size() > 1) {
			logger.severe("Found more than one user with username: " + user);
			throw new DAOException("Found more than one user with username: " + user);
		}
		
		try {
			QueryDAO qd = DAOFactory.getInstance().getQueryDAO();
			for (User u : users) {
				int userId = u.getId();
				RemoteQueryResultsInterface res = qd.retrieve("user = " + userId, "");
				List<Query> queries = res.getItems(0, res.getNumItems());
				for (Query q : queries)
					qd.delete(q.getQueryID());
			}
		}
		catch (QueryRetrieveException e) {
			throw new DAOException("Unable to retreive users", e);
		}
		catch (RemoteException e) {
			throw new DAOException("Unable to retreive users", e);
		}
		
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			Object[] parameters = {username.trim().toLowerCase()};
			ps = sqlStatements.buildSQLStatement(conn, DELETE_USER_KEY, parameters);
			ps.executeUpdate();
			conn.commit();
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Cannot delete the User", e);
		}
		catch (IOException e) {
			throw new DAOException("Cannot delete the User", e);
		}
		finally {
			DBUtils.closeStatement(ps);
			DBUtils.closeConnection(conn);
		}
		
	}
	
	/**
	 * Given a pair of username and password, determine whether or not they match
	 * the records in the database.
	 * 
	 * @return a User with its access level if matched, null otherwise
	 */
	public User checkPassword(String username, String passwordHash) throws DAOException {
		if (username == null || passwordHash == null)
			throw new IllegalArgumentException("Invalid userId/password (null)");
		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs;
		
		try {
			conn = dataSource.getConnection();
			Object[] parameters = {username.trim().toLowerCase(), passwordHash};
			ps = sqlStatements.buildSQLStatement(conn, CHECK_PASSWORD_KEY, parameters);
			rs = ps.executeQuery();
			
			// Return a User hash comparison is correct
			if (rs.next()) {
				return new User(rs.getInt(2), username, null, User.AccessLevel.valueOf(rs.getString(3)));
			}
			
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Cannot add User", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Cannot add User", e);
		}
		finally {
			DBUtils.closeStatement(ps);
			DBUtils.closeConnection(conn);
		}
		
		// If hashes are different or user doesn't exist, return null
		return null;
	}
	
}
