package au.edu.usyd.corona.server.session;


/**
 * Thrown if there is a problem retrieving result tables
 * 
 * @author Raymes Khoury
 * 
 */
@SuppressWarnings("serial")
public class QueryRetrieveException extends RetrieveRemoteException {
	public QueryRetrieveException(String message) {
		super(message);
	}
}
