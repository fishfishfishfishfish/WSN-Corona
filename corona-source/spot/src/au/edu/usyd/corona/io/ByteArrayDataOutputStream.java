package au.edu.usyd.corona.io;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

/**
 * Wrapper class to combine {@link DataOutputStream} and
 * {@link ByteArrayOutputStream} into one class
 * 
 * @author Raymes Khoury
 * @see DataOutputStream
 * @see ByteArrayOutputStream
 * @see ByteArrayDataInputStream
 */
public class ByteArrayDataOutputStream extends DataOutputStream {
	public ByteArrayDataOutputStream() {
		super(new ByteArrayOutputStream());
	}
	
	public byte[] getBytes() {
		return ((ByteArrayOutputStream) out).toByteArray();
	}
}
