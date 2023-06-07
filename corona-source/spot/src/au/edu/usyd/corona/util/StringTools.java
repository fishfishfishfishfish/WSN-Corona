package au.edu.usyd.corona.util;


import java.util.Vector;

/**
 * A set of String manipulation utilities, many of which are implementations of
 * common functions excluded from the Squawk libraries
 * 
 * @author Tim Dawborn
 */
public class StringTools {
	private StringTools() {
		// hidden constructor
	}
	
	/**
	 * Explodes a String into an array by the provided delimiter Complexity: O(n)
	 * relative to the input string
	 * 
	 * @param input the String to split up
	 * @param delimiter the delimiter to split the string up by
	 * @return the components of the input string split up by the given delimiter
	 */
	public static String[] explode(String input, char delimiter) {
		if (input == null)
			return new String[0];
		final Vector parts = new Vector();
		boolean wasPrevious = false;
		int start = 0;
		
		for (int i = 0; i != input.length(); ++i) {
			if (input.charAt(i) == delimiter) {
				if (!wasPrevious)
					parts.addElement(input.substring(start, i));
				start = i + 1;
				wasPrevious = true;
			}
			else
				wasPrevious = false;
		}
		if (!wasPrevious)
			parts.addElement(input.substring(start));
		
		return CollectionTools.vectorToArrayS(parts);
	}
	
	/**
	 * Explodes a String into an array by the provided delimiter
	 * 
	 * @param input
	 * @param delimiter
	 * @return the split version of the input string by the delimiter
	 */
	public static String[] explode(String input, String delimiter) {
		final Vector parts = new Vector();
		boolean wasPrevious = false;
		int i, start = 0;
		int maxindex = input.length() - delimiter.length() + 1;
		
		if (input.length() == 0)
			return new String[]{""};
		else if (delimiter.length() == 0)
			return new String[]{input};
		
		for (i = 0; i < maxindex; ++i) {
			if (input.substring(i, i + delimiter.length()).equals(delimiter)) {
				if (!wasPrevious)
					parts.addElement(input.substring(start, i));
				start = i + delimiter.length();
				i += delimiter.length() - 1;
				wasPrevious = true;
			}
			else
				wasPrevious = false;
		}
		if ((i < input.length()) && !input.substring(start).equals(delimiter))
			parts.addElement(input.substring(start));
		else if (!wasPrevious)
			parts.addElement(input.substring(start));
		
		return CollectionTools.vectorToArrayS(parts);
	}
	
	/**
	 * Check if the the specified char is contained in the string. Complexity:
	 * O(n) on the length of parameter string
	 * 
	 * @param s The String to search from.
	 * @param c The char to search for.
	 * @return true if the char is contained in the string.
	 */
	public static boolean contains(String s, char c) {
		char[] chars = s.toCharArray();
		for (int i = 0; i != chars.length; i++)
			if (c == chars[i])
				return true;
		return false;
	}
	
	/**
	 * Implode's all the items in the given array into one single string
	 * separated by the provided delimiter
	 * 
	 * @param items the items to be joined by the delimiter
	 * @param delimiter the delimiter to join all the objects together with
	 * @return string of "items[i] + delimiter" for each i, without a trailing
	 * delimiter
	 */
	public static String implode(Object[] items, String delimiter) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i != items.length; i++) {
			if (i != 0)
				result.append(delimiter);
			result.append(items[i]);
		}
		return result.toString();
	}
	
	/**
	 * @param items
	 * @param delimiter
	 * @return the array joined by the given delimter
	 * @see #implode(Object[], String)
	 */
	public static String implode(Object[] items, char delimiter) {
		return implode(items, "" + delimiter);
	}
	
	/**
	 * Multiplies a given string a given number of times, and returns the result
	 * 
	 * @param src the string to multiply
	 * @param factor the number of times to multiply the input string
	 * @return the multiplied input string
	 */
	public static String multiply(String src, int factor) {
		StringBuffer out = new StringBuffer(src.length() * factor);
		for (int i = 0; i != factor; i++)
			out.append(src);
		return out.toString();
	}
	
	/**
	 * Replaces all occurences of find with replace in the source String src
	 * 
	 * @param src
	 * @param find
	 * @param replace
	 * @return replaced string
	 * @see #implode(Object[], char)
	 * @see #explode(String, String)
	 */
	public static String replaceAll(String src, char find, String replace) {
		String out = implode(explode(src, find), replace);
		if ((src.length() > 1) && (src.charAt(src.length() - 1) == find))
			out += replace;
		return out;
	}
	
	/**
	 * Reverses a string by character, to get the keyword for the end of a block
	 * 
	 * @param s the input string
	 * @return the string reversed by character
	 */
	public static String reverse(String s) {
		final char[] original = s.toCharArray();
		final char[] reversed = new char[original.length];
		
		for (int i = 0; i != original.length; i++)
			reversed[reversed.length - i - 1] = original[i];
		return new String(reversed);
	}
	
}
