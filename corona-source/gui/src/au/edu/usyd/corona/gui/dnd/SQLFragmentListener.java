package au.edu.usyd.corona.gui.dnd;


/**
 * Listener model iterface to facilitate event driven SQL generation updates
 * from a {@link SQLFragmentGenerator} instance
 * 
 * @author Tim Dawborn
 */
public interface SQLFragmentListener {
	/**
	 * Called by the SQL generator when its internal SQL has changed. The
	 * generator passes itself in as the parameter to this method
	 * 
	 * @param generator the {@link SQLFragmentGenerator} instance which has
	 * changed
	 */
	public void actUponChange(SQLFragmentGenerator generator);
}
