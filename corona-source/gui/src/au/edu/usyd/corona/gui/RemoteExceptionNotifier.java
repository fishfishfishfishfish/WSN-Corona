package au.edu.usyd.corona.gui;


import java.rmi.RemoteException;

/**
 * When a {@link RemoteException} is thrown at any point in the GUI, an
 * implementation of this class should be called (
 * {@link #notifyHandler(RemoteException)}) to handle the exception. A
 * RemoteException means something has died between the GUI and the RMI server,
 * and so we should shutdown the GUI.
 * 
 * @author Tim Dawborn
 */
public interface RemoteExceptionNotifier {
	public void notifyHandler(RemoteException e);
	
	public void notifyHandlerDelayed();
	
	public boolean isDelayed();
}
