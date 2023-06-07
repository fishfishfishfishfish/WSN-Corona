package au.edu.usyd.corona.server.util;


/**
 * An unique integer generator. ID numbers start at 0 and reaches a maximum of
 * 2^64 = 214783647. Calling next() after this will throw a
 * IntegerOverflowException.
 * 
 * @author Edmund Tse
 */
public class IDGenerator {
	private int current;
	
	public IDGenerator() {
		current = 0;
	}
	
	public IDGenerator(int start) {
		current = start;
	}
	
	/**
	 * Gets the next ID
	 * 
	 * @return an integer representing the next serial number
	 * @throws IntegerOverflowException
	 */
	public synchronized int next() throws IntegerOverflowException {
		//checks the id isnt maxed out
		if (current == Integer.MAX_VALUE) {
			reset();
			throw new IntegerOverflowException();
		}
		
		//increments the id for the group and returns
		current++;
		return current;
	}
	
	/**
	 * Resets the id to zero
	 */
	public synchronized void reset() {
		current = 0;
	}
	
	/**
	 * Set the id to a given value
	 * 
	 * @param val the value to set the current ID to
	 */
	public synchronized void set(int val) {
		current = val;
	}
	
	/**
	 * Return the current id
	 * 
	 * @return the current id
	 */
	public synchronized int current() {
		return current;
	}
	
	@SuppressWarnings("serial")
	private class IntegerOverflowException extends RuntimeException {
	}
}
