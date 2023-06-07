package au.edu.usyd.corona.sensing;


import au.edu.usyd.corona.types.IntType;
import au.edu.usyd.corona.types.ValueType;

/**
 * A sensor which returns 1 always. This is used internally by SRDB when a count
 * column is needed for queries which contain aggregates (<i>COUNT</i> and
 * <i>AVG</i> aggregates).
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
class CountSensor implements Sensor {
	
	public String getSensorName() {
		return "_count";
	}
	
	public ValueType sense() {
		return new IntType(1);
	}
}
