package au.edu.usyd.corona.gui.dnd;


/**
 * Any fragment which returns some portion of an SQL query implements this
 * 
 * @author Tim Dawborn
 */
public interface SQLFragmentGenerator {
	/**
	 * Returns the fragment of SQL generated by the component
	 * 
	 * @return the fragment of an SQL query
	 */
	public String getSQLFragment();
	
	public void subscribeSQLFragmentGeneratorListener(SQLFragmentListener listener);
}
