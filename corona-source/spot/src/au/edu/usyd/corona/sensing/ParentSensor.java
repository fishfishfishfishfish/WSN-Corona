package au.edu.usyd.corona.sensing;


import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.types.IEEEAddressType;
import au.edu.usyd.corona.types.ValueType;

/**
 * A sensor which returns the address of the parent node of the current SunSPOT,
 * as a {@link IEEEAddressType} data type.
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
class ParentSensor implements Sensor {
	
	public String getSensorName() {
		return "parent";
	}
	
	public ValueType sense() {
		if (Network.getInstance().getMode() == Network.MODE_SPOT)
			return new IEEEAddressType(Network.getInstance().getParentAddress());
		else
			return new IEEEAddressType(0);
	}
	
}
