package au.edu.usyd.corona.sensing;


import java.io.IOException;

import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.types.FloatType;
import au.edu.usyd.corona.types.ValueType;

import com.sun.spot.sensorboard.EDemoBoard;

/**
 * A sensor which reads from the accelerometer on the EDemoBoard and returns the
 * appropriate axis's value.
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
class AccelerationSensor implements Sensor {
	public static final byte TYPE_ACCX = 0;
	public static final byte TYPE_ACCY = 1;
	public static final byte TYPE_ACCZ = 2;
	
	private final byte axis;
	
	public AccelerationSensor(byte axis) {
		this.axis = axis;
	}
	
	public String getSensorName() {
		switch (axis) {
		case TYPE_ACCX:
			return "x";
		case TYPE_ACCY:
			return "y";
		case TYPE_ACCZ:
			return "z";
		}
		return null;
	}
	
	public ValueType sense() throws IOException {
		if (Network.getInstance().getMode() != Network.MODE_SPOT)
			return new FloatType(0);
		
		switch (axis) {
		case TYPE_ACCX:
			return new FloatType((float) EDemoBoard.getInstance().getAccelerometer().getAccelX());
		case TYPE_ACCY:
			return new FloatType((float) EDemoBoard.getInstance().getAccelerometer().getAccelY());
		case TYPE_ACCZ:
			return new FloatType((float) EDemoBoard.getInstance().getAccelerometer().getAccelZ());
		}
		return null;
	}
	
}
