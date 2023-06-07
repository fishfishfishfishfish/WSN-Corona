package au.edu.usyd.corona.gui.cache;


import java.rmi.RemoteException;

import org.apache.jcs.access.exception.CacheException;

import au.edu.usyd.corona.gui.GUIManager;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteUserResultsInterface;
import au.edu.usyd.corona.server.session.UserAccessException;
import au.edu.usyd.corona.server.session.UserRetrieveException;
import au.edu.usyd.corona.server.session.notifier.NotifierID.NotifierType;
import au.edu.usyd.corona.server.user.User;

/**
 * A cache for {@link User} objects obtained from the server via RMI
 * 
 * @author Tim Dawborn
 */
public class UserCache extends AbstractCache<User, Integer, UserRetrieveException, RemoteUserResultsInterface> {
	UserCache() throws CacheException {
		super("users", 10, NotifierType.USERS_TABLE_NOTIFIER);
	}
	
	@Override
	protected RemoteUserResultsInterface createProxy(String ordering) throws RemoteException, UserRetrieveException {
		try {
			return GUIManager.getInstance().getRemoteSessionInterface().getUsers("", ordering);
		}
		catch (UserAccessException e) {
			logger.warning(e.getMessage());
		}
		return null;
	}
	
	@Override
	protected Integer getKeyForObject(User object) {
		return object.getId();
	}
	
	@Override
	protected User getFakeInstance() {
		return new User("");
	}
}
