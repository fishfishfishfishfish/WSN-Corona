package au.edu.usyd.corona.gui.cache;


import java.rmi.RemoteException;
import java.util.logging.Logger;

import org.apache.jcs.access.exception.CacheException;

/**
 * A wrapper around all the RMI caches to perform the same function on all of
 * them. This class also keeps reference to the singleton references of each of
 * these caches.
 * 
 * @author Tim Dawborn
 */
public class CacheManager {
	private static final Logger logger = Logger.getLogger(CacheManager.class.getCanonicalName());
	private static CacheManager instance;
	
	private final QueryCache queryCache;
	private final UserCache userCache;
	private final ResultCache resultCache;
	
	static {
		try {
			instance = new CacheManager();
		}
		catch (Exception e) {
			logger.severe(e.getMessage());
			System.exit(1);
		}
	}
	
	private CacheManager() throws CacheException, RemoteException {
		queryCache = new QueryCache();
		userCache = new UserCache();
		resultCache = new ResultCache();
	}
	
	public static final CacheManager getInstance() {
		return instance;
	}
	
	public void initCaches() throws RemoteException {
		queryCache.subscribeToNotifications();
		userCache.subscribeToNotifications();
	}
	
	public void clearCaches() {
		queryCache.clearCache();
		userCache.clearCache();
		resultCache.clearCache();
	}
	
	public QueryCache getQueryCache() {
		return queryCache;
	}
	
	public UserCache getUserCache() {
		return userCache;
	}
	
	public ResultCache getResultCache() {
		return resultCache;
	}
}
