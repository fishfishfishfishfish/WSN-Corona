package au.edu.usyd.corona.server.session;


import java.rmi.Remote;
import java.rmi.RemoteException;

import au.edu.usyd.corona.server.user.AccessDeniedException;

/**
 * This class handles RemoteSessions.
 */
public interface RemoteSessionManagerInterface extends Remote {
	/**
	 * Tries to authenticate with the server.
	 * 
	 * @param username The username of the user to log in as
	 * @param password The password of the user to log in as
	 * @return a RemoteSessionInterface if authentication successful
	 * 
	 * @throws RemoteException If there is a problem connecting to the Desktop
	 * @throws AccessDeniedException If the user is denied access
	 */
	public RemoteSessionInterface login(String username, String password) throws RemoteException, AccessDeniedException;
	
}
