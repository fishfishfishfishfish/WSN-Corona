package au.edu.usyd.corona.server.session;


/**
 * Something has gone wrong while interacting with the User data access object.
 * 
 * @author Edmund Tse
 */
@SuppressWarnings("serial")
public class UserAccessException extends Exception {
	public UserAccessException() {
	}
	
	public UserAccessException(String message) {
		super(message);
	}
}
