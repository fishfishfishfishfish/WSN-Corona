package au.edu.usyd.corona.gui.cache;


/**
 * Classes which wish to be notified about when the contents of the cache has
 * been updated from RMI should implement this interface, and then subscribe to
 * a {@link CacheChangeNotifier} instance via the
 * {@link CacheChangeNotifier#addChangeListener(CacheChangeListener)} method.
 * 
 * @author Tim Dawborn
 */
public interface CacheChangeListener {
	public void cacheChanged();
}
