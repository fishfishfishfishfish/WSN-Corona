package au.edu.usyd.corona.compression;


import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZStream;

/**
 * A compression implementation using the <a
 * href="http://www.jcraft.com/jzlib/index.html">JZlib</a> library that we cut
 * down to remove features that we would not use.
 * 
 * @author Tim Dawborn
 */
public class JZlibCompressor implements Compressor {
	
	public byte[] compress(final byte[] bytes) {
		ZStream stream = new ZStream();
		stream.next_in = bytes;
		stream.next_in_index = 0;
		stream.next_out = new byte[64];
		stream.next_out_index = 0;
		
		stream.deflateInit(JZlib.Z_DEFAULT_COMPRESSION);
		
		while (stream.total_in != bytes.length) {
			stream.avail_in = stream.avail_out = 1; // force small buffers
			stream.deflate(JZlib.Z_NO_FLUSH);
		}
		
		while (true) {
			stream.avail_out = 1;
			int err = stream.deflate(JZlib.Z_FINISH);
			if (err == JZlib.Z_STREAM_END)
				break;
		}
		
		stream.deflateEnd();
		
		final byte[] out = new byte[(int) stream.total_out];
		System.arraycopy(stream.next_out, 0, out, 0, out.length);
		return out;
	}
	
	public byte[] decompress(final byte[] bytes) {
		ZStream stream = new ZStream();
		stream.next_in = bytes;
		stream.next_in_index = 0;
		stream.next_out = new byte[64];
		stream.next_out_index = 0;
		
		stream.inflateInit();
		
		while (stream.total_in != bytes.length) {
			stream.avail_in = stream.avail_out = 1; /* force small buffers */
			int err = stream.inflate(JZlib.Z_NO_FLUSH);
			if (err == JZlib.Z_STREAM_END)
				break;
		}
		
		stream.inflateEnd();
		
		final byte[] out = new byte[(int) stream.total_out];
		System.arraycopy(stream.next_out, 0, out, 0, out.length);
		return out;
	}
	
}
