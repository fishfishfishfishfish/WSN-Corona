package au.edu.usyd.corona.sensing;


import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.types.IEEEAddressType;
import au.edu.usyd.corona.types.ValueType;

/**
 * A sensor which returns the address of the current SunSPOT node as a
 * {@link IEEEAddressType} data type.
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
class NodeSensor implements Sensor {
	
	public String getSensorName() {
		return "node";
	}
	
	public ValueType sense() {
		return new IEEEAddressType(Network.getInstance().getMyAddress());
	}
}
