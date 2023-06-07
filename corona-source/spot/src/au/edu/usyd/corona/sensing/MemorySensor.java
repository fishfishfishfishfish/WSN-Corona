package au.edu.usyd.corona.sensing;


import au.edu.usyd.corona.types.ByteType;
import au.edu.usyd.corona.types.ValueType;

/**
 * A sensor which returns the RAM usage as a whole number percentage (range 0 to
 * 100) of the current SunSPOT. This sensor uses the @{link
 * {@link java.lang.Runtime#totalMemory()} and
 * {@link java.lang.Runtime#freeMemory()} methods to calculate this value. It
 * also requests a garbage collection to happen beforehand so that this value is
 * more accurate, via a call to {@link java.lang.Runtime#gc()}.
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
class MemorySensor implements Sensor {
	private static final long MAX_MEMORY = Runtime.getRuntime().totalMemory();
	
	public String getSensorName() {
		return "memory";
	}
	
	public ValueType sense() {
		Runtime.getRuntime().gc();
		return new ByteType((byte) (((MAX_MEMORY - Runtime.getRuntime().freeMemory()) * 100) / MAX_MEMORY));
	}
}
