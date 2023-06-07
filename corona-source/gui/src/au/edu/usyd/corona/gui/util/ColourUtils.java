package au.edu.usyd.corona.gui.util;


import java.awt.Color;

/**
 * Miscelanious utilities related to colours
 * 
 * @author Tim Dawborn
 */
public class ColourUtils {
	private ColourUtils() {
	}
	
	/**
	 * Creates a Color instance based off a RGB hex string, in the format
	 * 'RRGGBB' or '#RRGGBB'. If the string is invalid, null is returned
	 * 
	 * @param rgbHex the hex string
	 * @return the corresponding Color instance
	 */
	public static Color fromRGBHex(String rgbHex) {
		// check for correct length
		final int len = rgbHex.length();
		if (!(len == 6 || (len == 7 && rgbHex.charAt(0) == '#')))
			return null;
		
		// strip HTML hash
		if (len == 7)
			rgbHex = rgbHex.substring(1);
		
		try {
			int r = Integer.parseInt(rgbHex.substring(0, 2), 16);
			int g = Integer.parseInt(rgbHex.substring(2, 4), 16);
			int b = Integer.parseInt(rgbHex.substring(4, 6), 16);
			return new Color(r, g, b);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}
}
