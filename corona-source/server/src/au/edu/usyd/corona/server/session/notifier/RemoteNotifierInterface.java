package au.edu.usyd.corona.server.session.notifier;


import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * A RemoteNotifier encapsulates a notifier which gets called upon some event on
 * the Desktop.
 * 
 * @author Raymes Khoury
 * 
 */
public interface RemoteNotifierInterface extends Remote {
	
	/**
	 * Called upon some event on the Desktop. The ID of the notifier is made
	 * available.
	 * 
	 * @param id The ID of the notifier (source of the event)
	 * @throws RemoteException If there is a problem connecting to the client.
	 */
	public void update(NotifierID id) throws RemoteException;
	
}
