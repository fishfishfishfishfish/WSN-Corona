package au.edu.usyd.corona.server.user;


/**
 * Signals that access to some resource has been disallowed. This may be caused
 * by an incorrect username and password combination, or an attempt to invoke
 * methods that require higher access levels.
 * 
 * @author Edmund Tse
 */
@SuppressWarnings("serial")
public class AccessDeniedException extends SecurityException {
	public AccessDeniedException() {
	}
	
	public AccessDeniedException(String message) {
		super(message);
	}
}
