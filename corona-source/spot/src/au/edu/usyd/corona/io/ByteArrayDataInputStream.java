package au.edu.usyd.corona.io;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

/**
 * Wrapper class to combine {@link DataInputStream} and
 * {@link ByteArrayInputStream} into one class
 * 
 * @author Raymes Khoury
 * @see DataInputStream
 * @see ByteArrayInputStream
 * @see ByteArrayDataOutputStream
 */
public class ByteArrayDataInputStream extends DataInputStream {
	public ByteArrayDataInputStream(byte[] data) {
		super(new ByteArrayInputStream(data));
	}
}
