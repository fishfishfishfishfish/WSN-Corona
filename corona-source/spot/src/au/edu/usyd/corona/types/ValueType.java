package au.edu.usyd.corona.types;


import java.io.DataInput;
import java.io.DataOutput;

/**
 * This interface represents the type of a value stored in a table. It also
 * provides methods for encoding and decoding a value for transmission, as well
 * as combining a comparing values of this type. The operators (
 * {@link #add(ValueType)}, {@link #divide(ValueType)},
 * {@link #multiply(ValueType)}) should all return <b>references to new
 * instances</b> rather than modifying the state of either of the
 * {@link ValueType} objects involved. The {@link #negate()} operator is
 * modifying however, and should return a <b>reference to itself</b> after its
 * value has been "negated".
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
public interface ValueType {
	/**
	 * Pull a value from a ByteStream and convert it to a value of the type
	 * provided
	 * 
	 * @param b The byte stream to pull a value from
	 * @return The decoded value
	 */
	public ValueType decode(DataInput b);
	
	/**
	 * Encode a value into binary-form and store it in a ByteStream
	 * 
	 * @param b The ByteStream to append to
	 */
	public void encode(DataOutput b);
	
	/**
	 * Add two ValueType's together
	 * 
	 * @param v The value to add to the current ValueType
	 * @return The a reference to the new ValueType
	 * @throws InvalidOperationException
	 */
	public ValueType add(ValueType v) throws InvalidOperationException;
	
	/**
	 * Multiply a ValueType by another
	 * 
	 * @param v The value to multiply by the current ValueType
	 * @return The a reference to the new ValueType
	 * @throws InvalidOperationException
	 */
	public ValueType multiply(ValueType v) throws InvalidOperationException;
	
	/**
	 * Divide a ValueType by another
	 * 
	 * @param v The ValueType to divide by
	 * @return A reference to a new ValueType
	 * @throws InvalidOperationException
	 */
	public ValueType divide(ValueType v) throws InvalidOperationException;
	
	/**
	 * Negate the current value
	 * 
	 * @return The a reference to the new ValueType
	 * @throws InvalidOperationException
	 */
	public ValueType negate() throws InvalidOperationException;
	
	/**
	 * Determine whether the given ValueType equals the current
	 * 
	 * @param v The ValueType to compare to the current
	 * @return The a reference to the new ValueType
	 * @throws InvalidOperationException
	 */
	public boolean less(ValueType v) throws InvalidOperationException;
	
	/**
	 * Determine whether a given value is equal to the current
	 * 
	 * @param v The ValueType to compare to the current
	 * @return Whether or not the given ValueType equals the current
	 * @throws InvalidOperationException
	 */
	public boolean equals(ValueType v) throws InvalidOperationException;
	
	/**
	 * Gets the token grammar language representation of the current ValueType
	 * value
	 * 
	 * @return the tokens identifying this particular ValueType instance
	 */
	public String toTokens();
	
	/**
	 * Returns a representation of the value as a standard Java object that can
	 * be inserted into a DBMS through JDBC. A list of supported object types can
	 * be found at:
	 * http://java.sun.com/j2se/1.3/docs/guide/jdbc/getstart/mapping.html#1034737
	 * 
	 * @return A representation of this object as a standard Java object
	 */
	public Object toJDBCObject();
}
