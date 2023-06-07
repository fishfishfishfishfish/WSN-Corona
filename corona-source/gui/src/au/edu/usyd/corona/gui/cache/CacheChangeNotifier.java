package au.edu.usyd.corona.gui.cache;


/**
 * Classes which wish provide the ability to notify objects upon contents of
 * cache items from the RMI should implement this interface.
 * 
 * @author Tim Dawborn
 */
public interface CacheChangeNotifier {
	public void addChangeListener(CacheChangeListener listener);
	
	public void removeChangeListener(CacheChangeListener listener);
}
