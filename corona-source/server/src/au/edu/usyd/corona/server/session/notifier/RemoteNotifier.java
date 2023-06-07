package au.edu.usyd.corona.server.session.notifier;


import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * A RemoteNotifier encapsulates a notifier which gets called upon some event on
 * the GUI.
 * 
 * @author Raymes Khoury
 * 
 */
@SuppressWarnings("serial")
public class RemoteNotifier extends UnicastRemoteObject implements RemoteNotifierInterface {
	private final NotifierInterface update;
	
	public RemoteNotifier(NotifierInterface update) throws RemoteException {
		this.update = update;
	}
	
	public void update(NotifierID id) throws RemoteException {
		update.update(id);
	}
}
