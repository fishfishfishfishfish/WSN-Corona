package au.edu.usyd.corona.srdb;


/**
 * This class represents an operator which simply returns the given table
 * 
 * @author Raymes Khoury
 */
public class ReadOperator extends TableOperator {
	private final Table t;
	
	public ReadOperator(Table t) {
		this.t = t;
	}
	
	public Table eval(int epoch) {
		return t;
	}
	
	public StringBuffer toTokens() {
		// not needed
		return null;
	}
}
