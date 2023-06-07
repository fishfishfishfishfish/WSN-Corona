package au.edu.usyd.corona.gui.cache;


/**
 * Any cache should implement this interface
 * 
 * @author Tim Dawborn
 * 
 * @param <T> the type of object the cache is caching
 * @param <KeyT> the type of key used to index the object
 */
public interface CacheInterface<T, KeyT extends Comparable<? super KeyT>> extends CacheChangeNotifier {
	
	public T getObject(String ordering, KeyT key);
	
	public T getObjectAtIndex(String ordering, int index);
	
	public int getSize(String ordering);
	
	public void clearCache();
}
