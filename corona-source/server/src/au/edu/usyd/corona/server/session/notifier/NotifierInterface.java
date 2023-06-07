package au.edu.usyd.corona.server.session.notifier;


/**
 * This interface is implemented to allow Clients to be notified of events on
 * the Desktop
 * 
 * @author Raymes Khoury
 * 
 */
public interface NotifierInterface {
	public void update(NotifierID id);
}
