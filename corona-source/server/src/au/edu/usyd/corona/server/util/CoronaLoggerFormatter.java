package au.edu.usyd.corona.server.util;


import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * This class is responsible for routing java.util.logging log messages from the
 * Desktop program, through the au.edu.usyd.corona.util.Logger so that we have
 * consistent logging messages.
 * 
 * @author Tim Dawborn
 * 
 */
public class CoronaLoggerFormatter extends Formatter {
	@Override
	public String format(LogRecord record) {
		return record.getMessage();
	}
}
