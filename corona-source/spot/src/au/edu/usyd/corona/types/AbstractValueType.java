package au.edu.usyd.corona.types;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import au.edu.usyd.corona.grammar.TokenGrammarTokens;
import au.edu.usyd.corona.util.Logger;
import au.edu.usyd.corona.util.SPOTTools;

/**
 * Abstract base class implementation of {@link ValueType} for all the type
 * implementations in this package to extend from.
 * 
 * @author Tim Dawborn
 * @author Raymes Khoury
 */
abstract class AbstractValueType implements ValueType, TokenGrammarTokens {
	
	protected abstract void _decode(DataInput b) throws IOException;
	
	/**
	 * Pull a value from a ByteStream and convert it to a value of the type
	 * provided
	 * 
	 * @param b The byte stream to pull a value from
	 * @return The decoded value
	 */
	public final ValueType decode(DataInput b) {
		try {
			_decode(b);
		}
		catch (IOException e) {
			SPOTTools.reportError(e);
		}
		return this;
	}
	
	protected abstract void _encode(DataOutput b) throws IOException;
	
	/**
	 * Encode a value into binary-form and store it in a ByteStream
	 * 
	 * @param b The ByteStream to append to
	 */
	public final void encode(DataOutput b) {
		try {
			_encode(b);
		}
		catch (IOException e) {
			SPOTTools.reportError(e);
			Logger.logError(e.getMessage());
		}
	}
	
	/**
	 * Overrides the toString method so we can have some useful debugging output
	 */
	public abstract String toString();
	
	/**
	 * Overrides for use in hashed data structures
	 */
	public final boolean equals(Object o) {
		if (o instanceof ValueType)
			return toTokens().equals(((ValueType) o).toTokens());
		return false;
	}
	
	/**
	 * Overrides for use in hashed data structures
	 */
	public final int hashCode() {
		return toTokens().hashCode();
	}
}
