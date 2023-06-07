package au.edu.usyd.corona.scheduler;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Anything that is to be transmitted via the wireless needs to implement this
 * interface. This interface provides the methods used to dissassemble an object
 * for sending, and then for reassembling the same object at the other end.
 * 
 * @author Raymes Khoury
 */
public interface Transmittable {
	/**
	 * Used to get an encoding of the object, which later will be used by
	 * {@link #decode(DataInput)} to construct an equivalent object again
	 * 
	 * @param data the output stream to write the encoding to
	 * @throws IOException something goes wrong when using the {@link DataOutput}
	 */
	public void encode(DataOutput data) throws IOException;
	
	/**
	 * Used to take an encoding of an object generated by
	 * {@link #encode(DataOutput)} back into an equivalent object instance.
	 * 
	 * @param data the input stream to read the encoding from
	 * @throws IOException something goes wrong when using the {@link DataInput}
	 */
	public void decode(DataInput data) throws IOException;
}