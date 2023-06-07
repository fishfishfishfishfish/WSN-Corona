package au.edu.usyd.corona.gui.cache;


import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;

import au.edu.usyd.corona.gui.GUIManager;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteResultsInterface;
import au.edu.usyd.corona.server.session.RetrieveRemoteException;
import au.edu.usyd.corona.server.session.notifier.NotifierID;
import au.edu.usyd.corona.server.session.notifier.NotifierInterface;
import au.edu.usyd.corona.server.session.notifier.RemoteNotifier;
import au.edu.usyd.corona.server.session.notifier.NotifierID.NotifierType;

/**
 * Abstract base class implementation for some caches
 * 
 * @author Tim Dawborn
 * 
 * @param <T> the type of object to cache
 * @param <KeyT> the key into this type of object
 * @param <E> the type of remote exception that is thrown for this objects
 * activities
 * @param <P> the type of proxy object used to interact with the server via the
 * RMI
 */
abstract class AbstractCache<T, KeyT extends Comparable<? super KeyT>, E extends RetrieveRemoteException, P extends RemoteResultsInterface<T, E>> implements NotifierInterface, CacheInterface<T, KeyT> {
	protected static final Logger logger = Logger.getLogger(AbstractCache.class.getCanonicalName());
	
	protected final Collection<WeakReference<CacheChangeListener>> listeners = new ArrayList<WeakReference<CacheChangeListener>>();
	protected final Map<String, P> proxyMap = new HashMap<String, P>(); // ordering => proxy object
	protected final Map<String, JCS> cacheMap = new HashMap<String, JCS>(); // ordering => JCS cache object
	protected final Map<String, List<KeyT>> indexMap = new HashMap<String, List<KeyT>>(); // ordering => ordered keys
	protected final Map<String, Integer> sizeMap = new HashMap<String, Integer>(); // ordering => num rows
	protected final String CACHE_KEY;
	protected final int CACHE_LOAD_WINDOW;
	protected final NotifierType NOTIFIER_TYPE;
	
	protected AbstractCache(String cacheKey, int cacheLoadWindow, NotifierType notifierType) {
		this.CACHE_KEY = cacheKey + "|";
		this.CACHE_LOAD_WINDOW = cacheLoadWindow;
		this.NOTIFIER_TYPE = notifierType;
	}
	
	protected JCS getCache(String ordering) {
		try {
			return JCS.getInstance(CACHE_KEY + ordering);
		}
		catch (CacheException e) {
			logger.severe(e.getMessage());
			System.exit(1);
		}
		return null;
	}
	
	protected abstract P createProxy(String ordering) throws RemoteException, RetrieveRemoteException;
	
	private void updateIndicies(String ordering) throws RemoteException, RetrieveRemoteException {
		P proxy = proxyMap.get(ordering);
		if (proxy == null)
			return;
		List<KeyT> indices = new ArrayList<KeyT>();
		for (T o : proxy.getItems(0, proxy.getNumItems()))
			indices.add(getKeyForObject(o));
		indexMap.put(ordering, indices);
	}
	
	private void ensureOrdering(String ordering) throws RemoteException, RetrieveRemoteException {
		if (!proxyMap.containsKey(ordering)) {
			P proxy = createProxy(ordering);
			proxyMap.put(ordering, proxy);
			cacheMap.put(ordering, getCache(ordering));
			sizeMap.put(ordering, (proxy == null) ? 0 : proxy.getNumItems());
			updateIndicies(ordering);
		}
	}
	
	public synchronized int getSize(String ordering) {
		try {
			ensureOrdering(ordering);
			return sizeMap.get(ordering);
		}
		catch (RemoteException e) {
			GUIManager.getInstance().getRemoteExceptionNotifier().notifyHandler(e);
		}
		catch (RetrieveRemoteException e) {
			logger.warning(e.getMessage());
		}
		return 0;
	}
	
	protected abstract KeyT getKeyForObject(final T object);
	
	public synchronized T getObjectAtIndex(String ordering, int index) {
		try {
			ensureOrdering(ordering);
			return getObject(ordering, indexMap.get(ordering).get(index));
		}
		catch (RemoteException e) {
			GUIManager.getInstance().getRemoteExceptionNotifier().notifyHandler(e);
		}
		catch (RetrieveRemoteException e) {
			logger.warning(e.getMessage());
		}
		return getFakeInstance();
	}
	
	protected abstract T getFakeInstance();
	
	@SuppressWarnings("unchecked")
	public synchronized T getObject(String ordering, KeyT key) {
		try {
			// ensure proxy object and cache
			ensureOrdering(ordering);
			JCS cache = getCache(ordering);
			T object = (T) cache.get(key);
			if (object == null) {
				// load a window into the cache
				int index = indexMap.get(ordering).indexOf(key);
				int low = Math.max(0, index - CACHE_LOAD_WINDOW);
				List<T> objects = proxyMap.get(ordering).getItems(low, low + 2 * CACHE_LOAD_WINDOW);
				for (T obj : objects)
					cache.put(getKeyForObject(obj), obj);
				
				// fix result reference
				object = (T) cache.get(key);
			}
			return object;
		}
		catch (RemoteException e) {
			GUIManager.getInstance().getRemoteExceptionNotifier().notifyHandler(e);
		}
		catch (RetrieveRemoteException e) {
			logger.warning(e.getMessage());
		}
		catch (CacheException e) {
			logger.warning(e.getMessage());
		}
		return getFakeInstance();
	}
	
	public synchronized void update(NotifierID nid) {
		clearCache(true);
		
		// update our listeners to notify about this change
		updateListeners();
	}
	
	protected void updateListeners() {
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
	
	private void clearCache(boolean clearDataOnly) {
		for (JCS cache : cacheMap.values()) {
			try {
				cache.clear();
				cache.dispose();
			}
			catch (CacheException e) {
				logger.warning(e.getMessage());
			}
		}
		cacheMap.clear();
		for (List<KeyT> indicies : indexMap.values())
			indicies.clear();
		indexMap.clear();
		sizeMap.clear();
		proxyMap.clear();
		if (!clearDataOnly)
			listeners.clear();
	}
	
	public synchronized void clearCache() {
		clearCache(false);
	}
	
	void subscribeToNotifications() throws RemoteException {
		RemoteNotifier remoteNotifier = new RemoteNotifier(this);
		NotifierID nid = new NotifierID(NOTIFIER_TYPE);
		GUIManager.getInstance().getRemoteSessionInterface().addNotifier(nid, remoteNotifier);
	}
}
