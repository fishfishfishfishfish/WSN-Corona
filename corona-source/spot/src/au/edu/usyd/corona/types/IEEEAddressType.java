package au.edu.usyd.corona.types;


import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

import com.sun.spot.util.IEEEAddress;

/**
 * Class representing a nodes IEEE address in our type system. This value is a
 * 64 bit number.
 * 
 * @author Tim Dawborn
 * @author Raymes Khoury
 * @see IEEEAddress
 */
public class IEEEAddressType extends AbstractValueType {
	private long address;
	
	public IEEEAddressType() {
		this.address = 0;
	}
	
	public IEEEAddressType(long address) {
		this.address = address;
	}
	
	public IEEEAddressType(String address) {
		this.address = IEEEAddress.toLong(address);
	}
	
	public IEEEAddressType(DataInputStream b) {
		decode(b);
	}
	
	public ValueType add(ValueType a) throws InvalidOperationException {
		throw new InvalidOperationException("Cannot add types", this, a);
	}
	
	public ValueType multiply(ValueType a) throws InvalidOperationException {
		throw new InvalidOperationException("Cannot multiply types", this, a);
	}
	
	public ValueType divide(ValueType a) throws InvalidOperationException {
		throw new InvalidOperationException("Cannot divide types", this, a);
	}
	
	public boolean equals(ValueType a) throws InvalidOperationException {
		if (a instanceof IEEEAddressType)
			return ((IEEEAddressType) a).address == address;
		else if (a instanceof LongType)
			return ((LongType) a).getVal() == address;
		else
			throw new InvalidOperationException("Cannot equate types", this, a);
	}
	
	public boolean less(ValueType a) throws InvalidOperationException {
		if (a instanceof IEEEAddressType)
			return address < ((IEEEAddressType) a).address;
		else if (a instanceof LongType)
			return address < ((LongType) a).getVal();
		else
			throw new InvalidOperationException("Cannot comapre types", this, a);
	}
	
	public void _decode(DataInput b) throws IOException {
		address = b.readLong();
	}
	
	public void _encode(DataOutput b) throws IOException {
		b.writeLong(address);
	}
	
	public ValueType negate() {
		return new IEEEAddressType(-address);
	}
	
	public long getVal() {
		return address;
	}
	
	public String toTokens() {
		return T_DATA_TYPE_IEEE_ADDRESS + "" + T_GROUP_OPEN + Long.toString(address) + T_GROUP_CLOSE;
	}
	
	public String toString() {
		return IEEEAddress.toDottedHex(address);
	}
	
	public Object toJDBCObject() {
		return new Long(address);
	}
}
