package au.edu.usyd.corona.server.util;


import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import au.edu.usyd.corona.util.Logger;

/**
 * This class is responsible for routing java.util.logging log messages from the
 * Desktop program, through the au.edu.usyd.corona.util.Logger so that we have
 * consistent logging messages.
 * 
 * @author Tim Dawborn
 * 
 */
public class CoronaLoggerHandler extends Handler {
	private boolean isClosed;
	
	public CoronaLoggerHandler() {
		this(Logger.DEBUG);
	}
	
	public CoronaLoggerHandler(byte logLevel) {
		Logger.setLogLevel(logLevel);
		configure();
		isClosed = false;
	}
	
	private static Object loadClassInstance(String className) {
		Object o = null;
		try {
			Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(className);
			o = clazz.newInstance();
		}
		catch (Exception e) {
		}
		return o;
	}
	
	private void configure() {
		final LogManager manager = LogManager.getLogManager();
		final String cname = getClass().getName();
		final String pLevel = manager.getProperty(cname + ".level");
		final String pFilter = manager.getProperty(cname + ".filter");
		final String pFormatter = manager.getProperty(cname + ".formatter");
		final String pEncoding = manager.getProperty(cname + ".encoding");
		
		setLevel(pLevel == null ? Level.INFO : Level.parse(pLevel));
		setFilter(pFilter == null ? null : (Filter) loadClassInstance(pFilter));
		setFormatter(pFormatter == null ? new CoronaLoggerFormatter() : (Formatter) loadClassInstance(pFormatter));
		try {
			setEncoding(pEncoding);
		}
		catch (Exception e) {
			try {
				setEncoding(null);
			}
			catch (Exception ex2) {
			}
		}
	}
	
	@Override
	public void close() throws SecurityException {
		flush();
		isClosed = true;
	}
	
	@Override
	public void flush() {
		Logger.flush();
	}
	
	@Override
	public void publish(LogRecord record) {
		if (isClosed || Logger.getLogLevel() == Logger.NONE)
			return;
		
		final Level level = record.getLevel();
		final String message = getFormatter().format(record);
		if (level == Level.SEVERE)
			Logger.logError(message);
		else if (level == Level.WARNING)
			Logger.logWarning(message);
		else if (level == Level.INFO)
			Logger.logGeneral(message);
		else if (level == Level.CONFIG)
			Logger.logConfig(message);
		else if (level == Level.FINE || level == Level.FINER || level == Level.FINEST)
			Logger.logDebug(message);
	}
}
