package au.edu.usyd.corona.server.persistence.DAOinterface;


import au.edu.usyd.corona.server.session.UserRetrieveException;
import au.edu.usyd.corona.server.user.User;

/**
 * This is an interface for a proxy results object which is used by a Client to
 * access User objects on the Desktop.
 * 
 * @author Raymes Khoury
 * 
 */
public interface RemoteUserResultsInterface extends RemoteResultsInterface<User, UserRetrieveException> {
	
}
