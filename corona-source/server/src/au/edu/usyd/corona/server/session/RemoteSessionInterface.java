package au.edu.usyd.corona.server.session;


import java.rmi.Remote;
import java.rmi.RemoteException;

import au.edu.usyd.corona.server.grammar.Query;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteQueryResultsInterface;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteTableResultsInterface;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteUserResultsInterface;
import au.edu.usyd.corona.server.session.notifier.NotifierID;
import au.edu.usyd.corona.server.session.notifier.RemoteNotifierInterface;
import au.edu.usyd.corona.server.user.User;
import au.edu.usyd.corona.server.user.User.AccessLevel;

/**
 * Represents a Session for a single remote user. Provides access to the query
 * engine.
 * 
 * @author Raymes Khoury
 * @author Edmund Tse
 */
public interface RemoteSessionInterface extends Remote {
	/**
	 * Executes and saves the given query string on the query engine.
	 * 
	 * @param query The query string to execute
	 * @return A Query object which contains details of the query string which
	 * has been executed
	 * @throws RemoteException If there is a problem connecting to the Desktop
	 * @throws QueryExecuteException If there is a problem executing the query
	 */
	public Query executeQuery(String query) throws RemoteException, QueryExecuteException;
	
	/**
	 * This method retrieves the queries with a given criteria, in a given order.
	 * The criteria is a conjunction of expressions that includes fields from a
	 * Query object. E.g. id = 10 AND queryString = "route". The order by
	 * expression is a comma separated list of attribtues to order by. A subset
	 * of results can be returned using the start and end parameters. The results
	 * [start, end) will be retrieved
	 * 
	 * @param whereClause Criteria placed upon the number of Query's to retrieve
	 * @param orderBy The order to retrieve the queries in
	 * @return A list of queries matching the given criteria, in the given order
	 * @throws RemoteException If there is a problem connecting to the Desktop
	 * @throws QueryRetrieveException If there is a problem retrieving Query's
	 */
	public RemoteQueryResultsInterface retrieveQueries(String whereClause, String orderBy) throws RemoteException, QueryRetrieveException;
	
	/**
	 * Return rows of the result table which match the given SQL query.
	 * 
	 * @param sql The query to execute on a given table
	 * @return Rows that match the given criteria, in the given order
	 * @throws RemoteException If there is a problem connecting to the Desktop
	 * @throws ResultRetrieveException If there is a problem retrieving Results
	 */
	public RemoteTableResultsInterface retrieveRows(String sql) throws RemoteException, ResultRetrieveException;
	
	/**
	 * Add a notifier for a specific event
	 * 
	 * @param notifierID The ID of the notifier to add
	 * @param notifier A RemoteNotifier which contains the Notifier
	 * @throws RemoteException If there is a problem connecting to the Desktop
	 */
	public void addNotifier(NotifierID notifierID, RemoteNotifierInterface notifier) throws RemoteException;
	
	/**
	 * Returns a User object containing details of the current User who is logged
	 * into this session
	 * 
	 * @return A User object containing details of the current User who is logged
	 * into this session
	 * @throws RemoteException If there is a problem connecting to the Desktop
	 */
	public User getLoggedInUser() throws RemoteException;
	
	/**
	 * Return a Collection of User's in the system.
	 * 
	 * @param whereClause
	 * @param orderByClause
	 * 
	 * @return A Collection of User's in the system.
	 * @throws UserAccessException UserAccessException If the user doesn't have
	 * access to perform this operation
	 * @throws RemoteException RemoteException If there is a problem connecting
	 * to the Desktop
	 */
	public RemoteUserResultsInterface getUsers(String whereClause, String orderByClause) throws UserAccessException, RemoteException;
	
	/**
	 * Add a User to the system
	 * 
	 * @param username The username of the user
	 * @param password The password of the user
	 * @param accessLevel The AccessLevel of the user
	 * @throws UserAccessException If the user doesn't have access to perform
	 * this operation
	 * @throws RemoteException If there is a problem connecting to the Desktop
	 */
	public void addUser(String username, String password, AccessLevel accessLevel) throws UserAccessException, RemoteException;
	
	/**
	 * Updates various user details
	 * 
	 * @param userId the user to update
	 * @param username the username of the user to update
	 * @param password set to null if not updating password
	 * @param accessLevel set to null if not updating the access level
	 * @throws UserAccessException If the user doesn't have access to perform
	 * this operation
	 * @throws RemoteException If there is a problem connecting to the Desktop
	 */
	public void updateUser(int userId, String username, String password, AccessLevel accessLevel) throws UserAccessException, RemoteException;
	
	public void updatePassword(String oldPassword, String newPassword) throws UserAccessException, RemoteException;
	
	/**
	 * Remove a user from the system with the given username
	 * 
	 * @param username The username of the user to update
	 * @throws UserAccessException If the user doesn't have access to perform
	 * this operation
	 * @throws RemoteException If there is a problem connecting to the Desktop
	 */
	public void deleteUser(String username) throws UserAccessException, RemoteException;
	
	/**
	 * Remove the given notifier
	 * 
	 * @param notifierID The ID of the event associated with the Notifier
	 * @param notifier The Notifier to remove
	 * @throws RemoteException If there is a problem connecting to the Desktop
	 */
	public void removeNotifier(NotifierID notifierID, RemoteNotifierInterface notifier) throws RemoteException;
	
	/**
	 * Return the IEEE address of the basestation node
	 * 
	 * @return the long representation of the basestation's IEEE address
	 * @throws RemoteException If there is a problem connecting to the Desktop
	 */
	public long getBasestationIEEEAddress() throws RemoteException;
	
	/**
	 * Return an array of attribute names that exist in the query engine
	 * 
	 * @return An array of attribute names that exist in the query engine
	 * @throws RemoteException If there is a problem connecting to the Desktop
	 */
	public String[] getAttributeNames() throws RemoteException;
}
