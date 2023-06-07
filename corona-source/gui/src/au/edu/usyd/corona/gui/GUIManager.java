package au.edu.usyd.corona.gui;


import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFrame;

import au.edu.usyd.corona.server.session.RemoteSessionInterface;
import au.edu.usyd.corona.server.user.User;

/**
 * A class to keep items used all over the GUI (after login)
 * 
 * @author Tim Dawborn
 */
public class GUIManager {
	private static final GUIManager instance = new GUIManager();
	private final Collection<WeakReference<JFrame>> frames = new ArrayList<WeakReference<JFrame>>(); // all the JFrame instances created 
	private volatile RemoteExceptionNotifier remoteExceptionNotifier; // the RMI exception error notifier
	private volatile RemoteSessionInterface rmi; // the RMI instance
	private LoginFrame loginFrame;
	
	private GUIManager() {
		// hidden constructor
	}
	
	public static GUIManager getInstance() {
		return instance;
	}
	
	public synchronized void registerFrame(JFrame frame) {
		frames.add(new WeakReference<JFrame>(frame));
	}
	
	public synchronized void closeAllFrames() {
		for (WeakReference<JFrame> r : frames){
			if (r.get() != null) {
				r.get().dispose();
				r.get().setVisible(false);
			}
		}
		frames.clear();
		System.gc();
	}
	
	public synchronized void setRemoteExceptionNotifier(RemoteExceptionNotifier x) {
		remoteExceptionNotifier = x;
	}
	
	public synchronized RemoteExceptionNotifier getRemoteExceptionNotifier() {
		return remoteExceptionNotifier;
	}
	
	public synchronized void setRemoteSessionInterface(RemoteSessionInterface rmi) {
		this.rmi = rmi;
	}
	
	public synchronized RemoteSessionInterface getRemoteSessionInterface() {
		return rmi;
	}
	
	public synchronized void clear() {
		closeAllFrames();
		setRemoteExceptionNotifier(null);
		setRemoteSessionInterface(null);
	}
	
	public void setLoginFrame(LoginFrame f) {
		loginFrame = f;
	}
	
	public void closeMainFrame() {
		loginFrame.logout();
	}
	
	public User getLoggedInUser() {
		try {
			return rmi.getLoggedInUser();
		}
		catch (RemoteException e) {
			remoteExceptionNotifier.notifyHandler(e);
		}
		return null;
	}
}
