package au.edu.usyd.corona.server.persistence.DAOinterface;


import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import au.edu.usyd.corona.server.session.RetrieveRemoteException;

/**
 * An interface for result proxy objects from the DAO. When a Client executes a
 * query over the DAO, one of these objects is returned which contains a
 * compiled version of the query and allows them to access result Objects.
 * 
 * @author Raymes Khoury
 * 
 * @param <T> The class of object this proxy object is used to access
 * @param <E> The class of exception this proxy object throws on error
 */
public interface RemoteResultsInterface<T, E extends RetrieveRemoteException> extends Remote {
	/**
	 * Return the number of results in this proxy object
	 * 
	 * @return The number of results in this proxy object
	 * @throws RemoteException If there is a transmission error
	 * @throws E If there is some error accessing the database
	 */
	public int getNumItems() throws RemoteException, E;
	
	/**
	 * Return the actual objects in this proxy object. A start and end item are
	 * specified such that results can be paged. Will return objects in the range
	 * [startRow, endRow). If startRow == endRow, no objects will be returned.
	 * 
	 * @param start The first object to return
	 * @param end The last object to return
	 * @return The objects in this proxy object in the range [startRow, endRow)
	 * @throws RemoteException If there is a transmission error
	 * @throws E If there is some error accessing the database
	 */
	public List<T> getItems(int start, int end) throws RemoteException, E;
}
