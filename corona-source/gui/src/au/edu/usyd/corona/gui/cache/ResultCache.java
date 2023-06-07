package au.edu.usyd.corona.gui.cache;


import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;

import au.edu.usyd.corona.gui.GUIManager;
import au.edu.usyd.corona.server.grammar.Query;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteTableResultsInterface;
import au.edu.usyd.corona.server.session.ResultRetrieveException;
import au.edu.usyd.corona.server.session.notifier.NotifierID;
import au.edu.usyd.corona.server.session.notifier.NotifierInterface;
import au.edu.usyd.corona.server.session.notifier.RemoteNotifier;
import au.edu.usyd.corona.server.session.notifier.NotifierID.NotifierType;

/**
 * A cache for results of queries obtained from the server via RMI
 * 
 * @author Tim Dawborn
 */
public class ResultCache implements NotifierInterface {
	private static final int CACHE_LOAD_WINDOW = 60; // on each side
	private static final String CACHE_ROW_NUM_KEY = "numRows";
	private static final Logger logger = Logger.getLogger(ResultCache.class.getCanonicalName());
	
	private final Collection<WeakReference<CacheChangeListener>> listeners = new ArrayList<WeakReference<CacheChangeListener>>();
	private final Map<String, RemoteTableResultsInterface> proxyMap = new HashMap<String, RemoteTableResultsInterface>();
	private final Map<Integer, Set<String>> queryIdMap = new HashMap<Integer, Set<String>>();
	
	private RemoteNotifier remoteNotifier;
	
	ResultCache() {
		try {
			remoteNotifier = new RemoteNotifier(this);
		}
		catch (RemoteException e) {
			logger.warning(e.getMessage());
		}
	}
	
	private void ensureListenerForTable(Query query, String sql) {
		// makes sure we have a listener for the table id
		if (!queryIdMap.containsKey(query.getQueryID())) {
			try {
				NotifierID nid = new NotifierID(NotifierType.RESULT_TABLE_NOTIFIER, query.getQueryID());
				GUIManager.getInstance().getRemoteSessionInterface().addNotifier(nid, remoteNotifier);
				queryIdMap.put(query.getQueryID(), new HashSet<String>());
			}
			catch (RemoteException e) {
				GUIManager.getInstance().getRemoteExceptionNotifier().notifyHandler(e);
			}
		}
		
		// create a new proxy object if required
		if (!proxyMap.containsKey(sql)) {
			try {
				RemoteTableResultsInterface proxy = GUIManager.getInstance().getRemoteSessionInterface().retrieveRows(sql);
				proxyMap.put(sql, proxy);
			}
			catch (RemoteException e) {
				logger.warning(e.getMessage());
			}
			catch (ResultRetrieveException e) {
				logger.warning(e.getMessage());
			}
		}
		
		// ensures we know the mapping from sql to table id
		queryIdMap.get(query.getQueryID()).add(sql);
	}
	
	private JCS getCache(String query) {
		try {
			return JCS.getInstance(query);
		}
		catch (CacheException e) {
			logger.severe(e.getMessage());
			System.exit(1);
		}
		return null;
	}
	
	public synchronized int getNumCols(Query query, String sql) {
		try {
			ensureListenerForTable(query, sql);
			return proxyMap.get(sql).getNumCols();
		}
		catch (RemoteException e) {
			return 0;
		}
	}
	
	public synchronized int getNumRows(Query query, String sql) {
		try {
			ensureListenerForTable(query, sql);
			JCS cache = getCache(sql);
			Integer i = (Integer) cache.get(CACHE_ROW_NUM_KEY);
			if (i == null) {
				// create a new proxy object if required
				if (!proxyMap.containsKey(sql)) {
					RemoteTableResultsInterface proxy = GUIManager.getInstance().getRemoteSessionInterface().retrieveRows(sql);
					proxyMap.put(sql, proxy);
				}
				i = proxyMap.get(sql).getNumItems();
				cache.put(CACHE_ROW_NUM_KEY, i);
			}
			return i;
		}
		catch (RemoteException e) {
			logger.warning(e.getMessage());
			return 0;
		}
		catch (ResultRetrieveException e) {
			logger.warning(e.getMessage());
			return 0;
		}
		catch (CacheException e) {
			logger.warning(e.getMessage());
			return 0;
		}
	}
	
	public synchronized Class<?>[] getAttributes(Query query, String sql) {
		try {
			ensureListenerForTable(query, sql);
			return proxyMap.get(sql).getAttributes();
		}
		catch (RemoteException e) {
			return null;
		}
	}
	
	public synchronized String[] getColumnNames(Query query, String sql) {
		try {
			ensureListenerForTable(query, sql);
			return proxyMap.get(sql).getColumnNames();
		}
		catch (RemoteException e) {
			return null;
		}
	}
	
	public synchronized Object[] getResult(Query query, String sql, int rowNumber) {
		try {
			ensureListenerForTable(query, sql);
			return getCachedRow(sql, rowNumber);
		}
		catch (CacheException e) {
			logger.warning(e.getMessage());
		}
		catch (RemoteException e) {
			logger.warning(e.getMessage());
		}
		catch (ResultRetrieveException e) {
			logger.warning(e.getMessage());
		}
		return null;
	}
	
	public synchronized List<Object[]> getResults(Query query, String sql, int start, int end) {
		ensureListenerForTable(query, sql);
		List<Object[]> results = new ArrayList<Object[]>(end - start);
		try {
			for (int i = start; i != end; i++)
				results.add(getCachedRow(sql, i));
		}
		catch (CacheException e) {
			logger.warning(e.getMessage());
		}
		catch (RemoteException e) {
			logger.warning(e.getMessage());
		}
		catch (ResultRetrieveException e) {
			logger.warning(e.getMessage());
		}
		return results;
	}
	
	public synchronized List<Object[]> getAllResults(Query query, String sql) {
		ensureListenerForTable(query, sql);
		int nRows = getNumRows(query, sql);
		List<Object[]> results = new ArrayList<Object[]>(nRows);
		try {
			for (int i = 0; i != nRows; i++)
				results.add(getCachedRow(sql, i));
		}
		catch (CacheException e) {
			logger.warning(e.getMessage());
		}
		catch (RemoteException e) {
			logger.warning(e.getMessage());
		}
		catch (ResultRetrieveException e) {
			logger.warning(e.getMessage());
		}
		return results;
	}
	
	private synchronized Object[] getCachedRow(String query, int row) throws CacheException, RemoteException, ResultRetrieveException {
		// obtains from the cache
		JCS cache = getCache(query);
		Object[] cachedRow = (Object[]) cache.get(row);
		
		// if its not in the cache, 
		if (cachedRow == null) {
			// create a new proxy object if required
			if (!proxyMap.containsKey(query)) {
				RemoteTableResultsInterface proxy = GUIManager.getInstance().getRemoteSessionInterface().retrieveRows(query);
				proxyMap.put(query, proxy);
			}
			
			// insert the row into the cache
			int low = Math.max(0, row - CACHE_LOAD_WINDOW);
			List<Object[]> rows = proxyMap.get(query).getItems(low, low + 2 * CACHE_LOAD_WINDOW);
			for (int i = 0; i != rows.size(); i++)
				cache.put(low + i, rows.get(i));
			
			// update the cached row reference
			cachedRow = (Object[]) cache.get(row);
		}
		return cachedRow;
	}
	
	public void update(NotifierID nid) {
		// update the number of rows if applicable
		int queryId = nid.getId();
		if (!queryIdMap.containsKey(queryId))
			return;
		
		for (String sql : queryIdMap.get(queryId)) {
			try {
				getCache(sql).put(CACHE_ROW_NUM_KEY, proxyMap.get(sql).getNumItems());
			}
			catch (CacheException e) {
				logger.warning(e.getMessage());
			}
			catch (RemoteException e) {
				logger.warning(e.getMessage());
			}
			catch (ResultRetrieveException e) {
				logger.warning(e.getMessage());
			}
		}
		
		// notify all cache change listeners
		for (WeakReference<CacheChangeListener> r : listeners)
			if (r.get() != null)
				r.get().cacheChanged();
	}
	
	public synchronized void addChangeListener(CacheChangeListener listener) {
		listeners.add(new WeakReference<CacheChangeListener>(listener));
	}
	
	public synchronized void removeChangeListener(CacheChangeListener listener) {
		for (Iterator<WeakReference<CacheChangeListener>> it = listeners.iterator(); it.hasNext();) {
			WeakReference<CacheChangeListener> r = it.next();
			if (r.get() == listener) {
				it.remove();
				return;
			}
		}
	}
	
	public synchronized List<Object[]> getColumnsByFields(Query query, String... fields) {
		String selectClause = fields[0];
		for (int i = 1; i < fields.length; i++)
			selectClause += ", " + fields[i];
		return getAllResults(query, "SELECT " + selectClause + " FROM TABLE_" + query.getQueryID());
	}
	
	public synchronized void clearCache() {
		proxyMap.clear();
		queryIdMap.clear();
		listeners.clear();
	}
	
	public synchronized void removeCachesForQuery(Query query) {
		Set<String> sqls = queryIdMap.get(query.getQueryID());
		if (sqls != null) {
			// for every SQL statement for the query, remove its proxy object and its cache
			for (String sql : sqls) {
				proxyMap.remove(sql);
				try {
					JCS.getInstance(sql).clear();
				}
				catch (CacheException e) {
					logger.warning(e.getMessage());
				}
			}
			
			// remove the query from the cache entirely
			queryIdMap.remove(query.getQueryID());
		}
		
		// removes the remote notifier also
		try {
			NotifierID nid = new NotifierID(NotifierType.RESULT_TABLE_NOTIFIER, query.getQueryID());
			GUIManager.getInstance().getRemoteSessionInterface().removeNotifier(nid, remoteNotifier);
		}
		catch (RemoteException e) {
			GUIManager.getInstance().getRemoteExceptionNotifier().notifyHandler(e);
		}
		
		System.gc();
	}
}
