package au.edu.usyd.corona.server.persistence.DAOinterface;


import java.rmi.RemoteException;

import au.edu.usyd.corona.server.grammar.Query;

/**
 * A DAO for accessing Query objects.
 * 
 * @author Raymes
 * 
 */
public interface QueryDAO {
	/**
	 * Store a Query
	 * 
	 * @param query The Query to store
	 * @throws DAOException If there is a problem storing the Query
	 */
	public void insert(Query query) throws DAOException;
	
	/**
	 * Delete a Query
	 * 
	 * @param queryID The id of the Query to delete
	 * @throws DAOException If there is a problem deleting the Query
	 */
	public void delete(int queryID) throws DAOException;
	
	/**
	 * Retrieve a proxy result object containing Queries matching the given
	 * criteria. Criteria are specified in the style of an SQL WHERE clause, i.e.
	 * as conjunctions or disjunctions of [attribute][operator][value] sequences.
	 * The resultant order of the Querys is specified in the style of an SQL
	 * ORDER BY clause, i.e. a list of comma-separated attributes to sort by.
	 * Order of sorting can be specified with ASC or DESC. Also, only a subset of
	 * the Query objects can be selected (i.e. a certain page of Querys). This is
	 * achieved by specifying a start and end Query to retrieve, of the entire
	 * results.
	 * 
	 * @param whereClause The criteria of Query objects to return
	 * @param orderByClause The order in which to return Query objects
	 * @return A proxy result object with results matching the criteria given, in
	 * the order specified
	 * @throws DAOException If there is a problem retrieving the Querys
	 * @throws RemoteException If there is a problem creating the
	 * RemoteQueryResultsInterface
	 */
	public RemoteQueryResultsInterface retrieve(String whereClause, String orderByClause) throws DAOException, RemoteException;
	
	/**
	 * Returns the highest existing Query ID. Returns -1 if there is no existing
	 * Query
	 * 
	 * @return The highest existing Query ID. -1 if there are no Query's
	 * @throws DAOException If there is an error finding the highest Query ID
	 */
	public int getHighestQueryID() throws DAOException;
	
}
