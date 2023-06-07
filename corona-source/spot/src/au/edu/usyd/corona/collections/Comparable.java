package au.edu.usyd.corona.collections;


/**
 * An implementation of the standard java.lang.Comparable interface not included
 * within the Squawk API (<a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Comparable.html"
 * >http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Comparable.html</a>)
 * 
 * @author Raymes Khoury
 */
public interface Comparable {
	/**
	 * Compares this object with the specified object for order. Returns a
	 * negative integer, zero, or a positive integer as this object is less than,
	 * equal to, or greater than the specified object.
	 * 
	 * @param o the object to be compared
	 * @return a negative integer, zero, or a positive integer as this object is
	 * less than, equal to, or greater than the specified object.
	 */
	public int compareTo(Object o);
}
