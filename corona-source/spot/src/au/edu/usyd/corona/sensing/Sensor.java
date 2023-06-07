package au.edu.usyd.corona.sensing;


import java.io.IOException;

import au.edu.usyd.corona.types.ValueType;

/**
 * Anything which is "sense'able" has to implement this interface. When the SRDB
 * sense operation runs, it collects data from all of the registered sensors.
 * Each of those sensors is stored in the {@link SenseManager}, and implement
 * this interface.
 * 
 * @author Raymes Khoury
 */
public interface Sensor {
	/**
	 * @return the name of this sensor as to be used in the query language
	 * "<i>SELECT</i>" syntax, as well as what is returned as the column names of
	 * tables to users.
	 */
	public String getSensorName();
	
	/**
	 * Perform an actual sensing of data from the specific sensor object.
	 * 
	 * @return the value from sensing the sensor
	 * @throws IOException of reading from an actual sensor throws an IOException
	 */
	public ValueType sense() throws IOException;
}
