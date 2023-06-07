package au.edu.usyd.corona.server.persistence.DAOinterface;


import au.edu.usyd.corona.server.grammar.Query;
import au.edu.usyd.corona.server.session.QueryRetrieveException;

/**
 * This is an interface for a proxy results object which is used by a Client to
 * access Query objects on the Desktop.
 * 
 * @author Raymes Khoury
 * 
 */
public interface RemoteQueryResultsInterface extends RemoteResultsInterface<Query, QueryRetrieveException> {
	
}
