package au.edu.usyd.corona.server.session;


import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;

import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;
import au.edu.usyd.corona.server.persistence.DAOinterface.DAOFactory;
import au.edu.usyd.corona.server.persistence.DAOinterface.UserDAO;
import au.edu.usyd.corona.server.user.AccessDeniedException;
import au.edu.usyd.corona.server.user.User;
import au.edu.usyd.corona.server.util.Hasher;
import au.edu.usyd.corona.server.util.IDGenerator;

/**
 * This class handles RemoteSessions.
 */
@SuppressWarnings("serial")
public class RemoteSessionManager extends UnicastRemoteObject implements RemoteSessionManagerInterface {
	private static final Logger logger = Logger.getLogger(RemoteSessionManager.class.getCanonicalName());
	
	private final IDGenerator queryIDGenerator;
	
	public RemoteSessionManager(IDGenerator queryIDGenerator) throws RemoteException {
		this.queryIDGenerator = queryIDGenerator;
	}
	
	public RemoteSessionInterface login(String username, String password) throws RemoteException, AccessDeniedException {
		String passwordHash = Hasher.hash(password);
		
		try {
			UserDAO ud = DAOFactory.getInstance().getUserDAO();
			User user = ud.checkPassword(username, passwordHash);
			if (user != null)
				return new RemoteSession(user, queryIDGenerator);
		}
		catch (DAOException e) {
			logger.severe(e.getMessage());
			throw new AccessDeniedException("Cannot access Users information: " + e.getMessage());
		}
		
		throw new AccessDeniedException("Invalid username/password");
	}
}
