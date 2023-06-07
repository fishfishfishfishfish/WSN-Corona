package au.edu.usyd.corona.compression;


/**
 * Interface which provides compression to an array of bytes, producing an array
 * of bytes as the compressed version also. This interface is used primarily
 * when sending data via a network (wireless or wired), as well as when writing
 * data to persistant storage to help reduce the amount of space used
 * 
 * @author Tim Dawborn
 */
public interface Compressor {
	/**
	 * Compresses the input byte array
	 * 
	 * @param bytes the data to compress
	 * @return the compressed version of the input data
	 */
	public byte[] compress(final byte[] bytes);
	
	/**
	 * Decompresses the input which was compressed via {@link #compress(byte[])}
	 * 
	 * @param bytes the data to decompress
	 * @return the decompressed version of the input data (aka the original data)
	 */
	public byte[] decompress(final byte[] bytes);
}
