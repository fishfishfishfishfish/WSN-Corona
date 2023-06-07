package au.edu.usyd.corona.server.persistence.DAOinterface;


import java.rmi.RemoteException;

import au.edu.usyd.corona.server.session.ResultRetrieveException;

/**
 * This is an interface for a proxy results object which is used by a Client to
 * access rows of a Result table
 * 
 * @author Raymes Khoury
 * 
 */
public interface RemoteTableResultsInterface extends RemoteResultsInterface<Object[], ResultRetrieveException> {
	
	/**
	 * Return the number of columns in the result table
	 * 
	 * @return The number of columns in the result table
	 * @throws RemoteException If there is an error in transmission
	 */
	public int getNumCols() throws RemoteException;
	
	/**
	 * Return the types of the columns of the table as an array of classes
	 * 
	 * @return The types of the columns of the array
	 * @throws RemoteException If there is an error in transmission
	 */
	public Class<?>[] getAttributes() throws RemoteException;
	
	/**
	 * Return the names of the columns of the table as an array of String
	 * 
	 * @return The names of the columns of the table as an array of String
	 * @throws RemoteException If there is an error in transmission
	 */
	public String[] getColumnNames() throws RemoteException;
}
