package au.edu.usyd.corona.gui.util;


import java.awt.Color;
import java.awt.Font;

/**
 * This interface provides access to some useful constants used throughout the
 * system for drawing
 * 
 * @author Tim Dawborn
 */
public interface DrawingConstants {
	public static final Color LGREEN = ColourUtils.fromRGBHex("#8FFF6F");
	public static final Color LGREEN_L = LGREEN.brighter();
	
	public static final Color PALE_BLUE = ColourUtils.fromRGBHex("#8AF7F7");
	public static final Color PALE_BLUE_L = PALE_BLUE.brighter();
	
	public static final Color LIGHT_BLUE = ColourUtils.fromRGBHex("#5B99EB");
	public static final Color LIGHT_BLUE_L = LIGHT_BLUE.brighter();
	
	public static final Color BLUE = ColourUtils.fromRGBHex("#3B49EB");
	public static final Color BLUE_L = BLUE.brighter();
	
	public static final Color PALE_YELLOW = ColourUtils.fromRGBHex("#EBF732");
	public static final Color PALE_YELLOW_L = PALE_YELLOW.brighter();
	
	public static final Color RED = ColourUtils.fromRGBHex("#E83538");
	public static final Color RED_L = RED.brighter();
	
	public static final Color PURPLE = ColourUtils.fromRGBHex("#845CF1");
	public static final Color PURPLE_L = PURPLE.brighter();
	
	public static final Color BUBBLE_COLOUR = ColourUtils.fromRGBHex("#98C3DA");
	
	public static final Color NICE_BLUE = new Color(158, 176, 214);
	
	public static final Color BLACK = Color.BLACK;
	public static final Color WHITE = Color.WHITE;
	public static final Color LIGHT_GREY = Color.LIGHT_GRAY;
	
	public static final Font VSMALL_FONT = new Font("Verdana", Font.PLAIN, 11);
	public static final Font SMALL_FONT = new Font("Verdana", Font.PLAIN, 12);
	public static final Font MEDIUM_FONT = new Font("Verdana", Font.PLAIN, 18);
	public static final Font FONT = new Font("Verdana", Font.PLAIN, 25);
	
	public static final Font MONOSPACE_FONT = new Font("Courier New", Font.PLAIN, 16);
}
