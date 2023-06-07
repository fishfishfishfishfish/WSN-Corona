package au.edu.usyd.corona.types;


/**
 * This exception is thrown by the operators in our type system when an
 * operation is performed on two incompatable types. These two instances are
 * represented as in the {@link #first} and {@link #second} publicly accessable
 * instance variables.
 * 
 * @author Tim Dawborn
 */
public class InvalidOperationException extends Exception {
	public final ValueType first;
	public final ValueType second;
	
	public InvalidOperationException(String msg, ValueType first, ValueType second) {
		super(msg);
		this.first = first;
		this.second = second;
	}
}
