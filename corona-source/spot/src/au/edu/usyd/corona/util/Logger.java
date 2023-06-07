package au.edu.usyd.corona.util;


import java.io.PrintStream;

/**
 * A basic logging utility implementation for logging information to
 * PrintStream's on a severity level basis
 * 
 * @author Tim Dawborn
 */
public class Logger {
	public static final byte NONE = 0;
	public static final byte ERROR = 10;
	public static final byte WARNING = 20;
	public static final byte GENERAL = 30;
	public static final byte CONFIG = 40;
	public static final byte DEBUG = 50;
	public static final byte ALL = Byte.MAX_VALUE;
	
	private static final Object mutex = new Object();
	
	private static PrintStream out = System.err;
	private static byte logLevel = CONFIG;
	private static byte previousLogLevel;
	
	private Logger() {
		// hidden constructor; static access only
	}
	
	private static boolean checkSeverity(byte severity) {
		return severity <= logLevel;
	}
	
	public static void flush() {
		out.flush();
	}
	
	public static void endLogging() {
		out.flush();
		out.close();
	}
	
	public static void logConfig(String message) {
		logMessage(" [CONFIG]: ", message, CONFIG);
	}
	
	public static void logDebug(String message) {
		logMessage(" [DEBUG]: ", message, DEBUG);
	}
	
	public static void logError(String message) {
		logMessage(" [ERROR]: ", message, ERROR);
	}
	
	public static void logGeneral(String message) {
		logMessage(" [CORONA]: ", message, GENERAL);
	}
	
	public static void logWarning(String message) {
		logMessage(" [WARNING]: ", message, WARNING);
	}
	
	private static void logMessage(String prefix, String message, byte severity) {
		//checks if the outputs severity is of equal or higher importance to the chosen output level
		if (checkSeverity(severity)) {
			// mutex for output write access
			synchronized (mutex) {
				//if the current log severity is not the same as the previously outputted one, add a blank line
				if (previousLogLevel != severity) {
					previousLogLevel = severity;
					out.println();
				}
				
				//pads all new lines with enough spacing for them to align correctly with the prefix
				message = StringTools.replaceAll(message, '\n', "\n" + StringTools.multiply(" ", prefix.length()));
				//				out.print(Thread.currentThread().getName());
				out.print(prefix);
				out.println(message);
				out.flush();
			}
		}
	}
	
	public static void setLogLevel(byte logLevel) {
		Logger.logLevel = logLevel;
	}
	
	public static void setOut(PrintStream out) {
		Logger.out = out;
	}
	
	public static byte getLogLevel() {
		return logLevel;
	}
}
