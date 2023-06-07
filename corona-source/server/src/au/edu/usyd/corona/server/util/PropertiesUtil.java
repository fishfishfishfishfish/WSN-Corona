package au.edu.usyd.corona.server.util;


import java.io.IOException;
import java.util.Properties;

/**
 * Collection of helper function(s) related to accessing a Properties file.
 * 
 * @author Edmund Tse
 */
public class PropertiesUtil {
	
	/**
	 * Safety method for obtaining properties from a Properties object.
	 * Non-existent properties usually return null but we throw an exception in
	 * that case.
	 * 
	 * @param key The key of the property to return
	 * @param properties The Properties object to find the key in
	 * @return The String that matches the key of the property in the Properties
	 * object
	 * @throws IOException If the key does not exist
	 */
	public static String getProperty(String key, Properties properties) throws IOException {
		String res = properties.getProperty(key);
		if (res == null)
			throw new IOException("Invalid " + key + " property");
		return res;
	}
}
