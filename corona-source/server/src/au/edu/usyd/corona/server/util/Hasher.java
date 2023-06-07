package au.edu.usyd.corona.server.util;


import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import sun.misc.BASE64Encoder;

/**
 * Wrapper class around the <a href=
 * "http://java.sun.com/j2se/1.5.0/docs/api/java/security/package-summary.html"
 * >java.security.*</a> hashing functions.
 * 
 * @author Edmund Tse
 * @see MessageDigest
 */
public final class Hasher {
	private static String DEFAULT_HASHING_ALGORITHM = "SHA";
	
	/**
	 * Hashes the message using the default hashing algorithm.
	 * 
	 * @see #hash(String, String)
	 * @see #DEFAULT_HASHING_ALGORITHM
	 * @param message the message to hash
	 * @return the hashed message
	 */
	public static String hash(String message) {
		try {
			return hash(DEFAULT_HASHING_ALGORITHM, message);
		}
		catch (NoSuchAlgorithmException e) {
			throw new InternalError("Hashing algorithm SHA not supported");
		}
	}
	
	/**
	 * Hashes a given message using a particular algorithm
	 * 
	 * @param algorithm algorithm to use, e.g. MD5, SHA
	 * @param message the message to hash
	 * @return the hash
	 * @throws NoSuchAlgorithmException
	 */
	public static String hash(String algorithm, String message) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance(algorithm);
		
		try {
			md.update(message.getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException e) {
			throw new InternalError("Unable to encode the given message for hashing");
		}
		
		byte[] raw = md.digest();
		return new BASE64Encoder().encode(raw);
	}
}
