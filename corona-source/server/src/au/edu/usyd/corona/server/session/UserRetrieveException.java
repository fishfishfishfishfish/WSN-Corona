package au.edu.usyd.corona.server.session;


/**
 * Thrown if there is a problem processing a User
 * 
 * @author Raymes Khoury
 * 
 */
@SuppressWarnings("serial")
public class UserRetrieveException extends RetrieveRemoteException {
	public UserRetrieveException(String message) {
		super(message);
	}
}
