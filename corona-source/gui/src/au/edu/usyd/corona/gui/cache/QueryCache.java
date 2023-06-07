package au.edu.usyd.corona.gui.cache;


import java.rmi.RemoteException;

import org.apache.jcs.access.exception.CacheException;

import au.edu.usyd.corona.gui.GUIManager;
import au.edu.usyd.corona.server.grammar.Query;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteQueryResultsInterface;
import au.edu.usyd.corona.server.session.QueryRetrieveException;
import au.edu.usyd.corona.server.session.notifier.NotifierID.NotifierType;

/**
 * A cache for {@link Query} objects obtained from the server via RMI
 * 
 * @author Tim Dawborn
 */
public class QueryCache extends AbstractCache<Query, Integer, QueryRetrieveException, RemoteQueryResultsInterface> {
	QueryCache() throws CacheException {
		super("queries", 30, NotifierType.QUERIES_TABLE_NOTIFIER);
	}
	
	@Override
	protected RemoteQueryResultsInterface createProxy(String ordering) throws RemoteException, QueryRetrieveException {
		if (GUIManager.getInstance().getRemoteSessionInterface() == null)
			return null;
		final RemoteQueryResultsInterface x = GUIManager.getInstance().getRemoteSessionInterface().retrieveQueries("", ordering);
		return x;
	}
	
	@Override
	protected Integer getKeyForObject(final Query object) {
		return object.getQueryID();
	}
	
	@Override
	protected Query getFakeInstance() {
		return new Query();
	}
}
