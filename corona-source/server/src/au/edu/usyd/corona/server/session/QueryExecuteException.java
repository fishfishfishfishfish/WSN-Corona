package au.edu.usyd.corona.server.session;


/**
 * Thrown if there is a problem executing a Query
 * 
 * @author Raymes Khoury
 * 
 */
@SuppressWarnings("serial")
public class QueryExecuteException extends Exception {
	public QueryExecuteException() {
	}
	
	public QueryExecuteException(String message) {
		super(message);
	}
}
