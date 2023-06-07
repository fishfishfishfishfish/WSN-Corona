package au.edu.usyd.corona.util;


import au.edu.usyd.corona.scheduler.KillTask;
import au.edu.usyd.corona.scheduler.PropgateExceptionTask;
import au.edu.usyd.corona.scheduler.QueryTask;
import au.edu.usyd.corona.scheduler.SetPropertyTask;
import au.edu.usyd.corona.scheduler.TransmitResultsTask;
import au.edu.usyd.corona.types.BooleanType;
import au.edu.usyd.corona.types.ByteType;
import au.edu.usyd.corona.types.FloatType;
import au.edu.usyd.corona.types.IEEEAddressType;
import au.edu.usyd.corona.types.IntType;
import au.edu.usyd.corona.types.LongType;

/**
 * This class maps Classes to a byte which represents that class. This provides
 * an efficient way of transmitting type information for types which must be
 * transmitted.
 * 
 * @author Raymes Khoury
 */
public class ClassIdentifiers {
	private static final Class[] classes = new Class[]{ByteType.class, IntType.class, LongType.class, ByteType.class, BooleanType.class, IEEEAddressType.class, FloatType.class, KillTask.class, QueryTask.class, TransmitResultsTask.class, PropgateExceptionTask.class, SetPropertyTask.class};
	
	private ClassIdentifiers() {
		// hidden constructor
	}
	
	/**
	 * Does a reverse lookup to determine the Class of a given type-code
	 * 
	 * @param id The code of the type to retrieve
	 * @return The type desired
	 */
	public static Class getClass(byte id) {
		return classes[id];
	}
	
	/**
	 * Get the ID of a given class.
	 * 
	 * @param c The class to retrieve the ID of
	 * @return The id of the given class
	 */
	public static byte getID(Class c) {
		for (int i = 0; i != classes.length; i++)
			if (classes[i].equals(c))
				return (byte) i;
		throw new IllegalArgumentException("No byte value for the specified class " + c.getName());
	}
}
