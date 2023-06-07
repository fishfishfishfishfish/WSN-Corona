package au.edu.usyd.corona.server.session;


/**
 * Thrown if there is a problem retrieving results
 * 
 * @author Raymes Khoury
 */
@SuppressWarnings("serial")
public class ResultRetrieveException extends RetrieveRemoteException {
	public ResultRetrieveException(String msg) {
		super(msg);
	}
}
