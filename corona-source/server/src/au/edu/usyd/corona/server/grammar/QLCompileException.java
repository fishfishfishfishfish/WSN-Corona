package au.edu.usyd.corona.server.grammar;


/**
 * This exception is thrown when the compiler throws an error due to a semantic
 * check failing, or some other error with the query it was asked to compile.
 * 
 * @author Tim Dawborn
 */
@SuppressWarnings("serial")
public class QLCompileException extends Exception {
	public QLCompileException(String msg) {
		super(msg);
	}
}
