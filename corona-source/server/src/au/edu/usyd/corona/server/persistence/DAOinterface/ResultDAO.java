package au.edu.usyd.corona.server.persistence.DAOinterface;


import java.rmi.RemoteException;

import au.edu.usyd.corona.srdb.Table;

/**
 * This class represents a DAO for Table objects which are results from the
 * database. Results are returned as CachedResultSet objects such that the
 * client can utilise a resource as if they acquired it from a database.
 * 
 * @author Raymes Khoury
 * 
 */
public interface ResultDAO {
	/**
	 * Store a Table
	 * 
	 * @param table The Table to store
	 * @throws DAOException If there is a problem storing the Table
	 */
	public void insert(Table table) throws DAOException;
	
	/**
	 * Create a results table with the given column names and types
	 * 
	 * @param tableID The ID of the table to create
	 * @param attributeNames The names of the columns to create
	 * @param attributeTypes The types of the columns to create
	 * @throws DAOException If there is a problem creating the Table
	 */
	public void create(int tableID, String[] attributeNames, Class<?>[] attributeTypes) throws DAOException;
	
	/**
	 * Delete a table from the database
	 * 
	 * @param tableID The id of the table to delete
	 * @throws DAOException If there is a problem deleting the Table
	 */
	public void delete(int tableID) throws DAOException;
	
	/**
	 * Return a result proxy object containing rows of a result table which match
	 * the given SQL query.
	 * 
	 * 
	 * @param sql The query to execute on a given table
	 * @return CachedResultSet of the required rows
	 * @throws DAOException If there is a problem retrieving the Table rows
	 * @throws RemoteException If there is a problem creating the TableResults
	 */
	public RemoteTableResultsInterface retrieve(String sql) throws DAOException, RemoteException;
	
}
