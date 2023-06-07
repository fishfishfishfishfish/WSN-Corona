package au.edu.usyd.corona.server.persistence.DAOinterface;


import java.rmi.RemoteException;

import au.edu.usyd.corona.server.user.User;
import au.edu.usyd.corona.server.user.User.AccessLevel;

/**
 * This class represents a DAO for user in the system.
 * 
 * @author Edmund Tse
 */
public interface UserDAO {
	static final String DEFAULT_ADMIN = "admin";
	static final String DEFAULT_ADMIN_PASSWORD = "password";
	
	/**
	 * Tests if a given username exists in the system
	 * 
	 * @param username The name of the user
	 * @return true if the username exists; false otherwise
	 * @throws DAOException If there is a problem accessing the data source
	 */
	public boolean userExists(String username) throws DAOException;
	
	/**
	 * Gets all users in the system.
	 * 
	 * @return a collection of Users corresponding to users in the system
	 * @throws DAOException If there is a problem accessing the data source
	 * @throws RemoteException If there is a problem creating the
	 * RemoteUserResultsInterface
	 */
	public RemoteUserResultsInterface retrieveUsers() throws DAOException, RemoteException;
	
	/**
	 * Retrieve a proxy result object containing Users matching the given
	 * criteria.
	 * 
	 * @param whereClause The conditions to apply to retrieved users
	 * @param orderByClause The order in which to retrieve users
	 * @return a collection of Users satisfying the given expression
	 * @throws DAOException If there is a problem accessing the data source
	 * @throws RemoteException If there is a problem creating the
	 * RemoteUserResultsInterface
	 */
	public RemoteUserResultsInterface retrieveUsers(String whereClause, String orderByClause) throws DAOException, RemoteException;
	
	/**
	 * Adds a new user to the system. The user's username, password and access
	 * levels must be set in the User object.
	 * 
	 * @param user The User object to add to the DAO
	 * @throws DAOException If there is a problem accessing the data source
	 */
	public void addUser(User user) throws DAOException;
	
	/**
	 * Adds a new user to the system, using the default access level
	 * 
	 * @param username The username of the user to add
	 * @param passwordHash The password of the user to add
	 * @throws DAOException If there is a problem accessing the data source
	 */
	public void addUser(String username, String passwordHash) throws DAOException;
	
	/**
	 * Adds a new user to the system
	 * 
	 * @param username The username of the user to add
	 * @param passwordHash The password of the user to add
	 * @param accessLevel The AccessLevel of the user to add
	 * @throws DAOException If there is a problem accessing the data source
	 */
	public void addUser(String username, String passwordHash, AccessLevel accessLevel) throws DAOException;
	
	/**
	 * Modifies the passwordHash and accessLevel of the given username
	 * 
	 * @param username the user to update
	 * @param passwordHash set to null if don't want to update it
	 * @param accessLevel set to null if don't want to update it
	 * @throws DAOException If there is a problem accessing the data source
	 */
	public void updateUser(String username, String passwordHash, AccessLevel accessLevel) throws DAOException;
	
	/**
	 * Modifies the passwordHash and accessLevel of the given username
	 * 
	 * @param userId the userId to update
	 * @param username set to null if don't want to update it
	 * @param passwordHash set to null if don't want to update it
	 * @param accessLevel set to null if don't want to update it
	 * @throws DAOException If there is a problem accessing the data source
	 */
	public void updateUser(int userId, String username, String passwordHash, AccessLevel accessLevel) throws DAOException;
	
	/**
	 * Removes the named user from the system
	 * 
	 * @param username The username of the user to delete
	 * @throws DAOException If there is a problem accessing the data source
	 */
	public void deleteUser(String username) throws DAOException;
	
	/**
	 * Checks the pair of username and password. NOTE: the password field of the
	 * returned User is empty.
	 * 
	 * @param username The name of the user whose password to check
	 * @param passwordHash The password of the user
	 * @return a User containing username and access level if the username and
	 * password combination is correct, null otherwise.
	 * @throws DAOException If there is a problem accessing the data source
	 */
	public User checkPassword(String username, String passwordHash) throws DAOException;
	
}
