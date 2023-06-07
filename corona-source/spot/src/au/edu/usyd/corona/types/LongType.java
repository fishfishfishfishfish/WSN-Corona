package au.edu.usyd.corona.types;


import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Class representing a normal byte value (signed 64 bit) in our type system.
 * 
 * @author Tim Dawborn
 * @author Raymes Khoury
 */
public class LongType extends AbstractValueType {
	private long val;
	
	public LongType() {
		this.val = 0;
	}
	
	public LongType(long val) {
		this.val = val;
	}
	
	public LongType(DataInputStream b) {
		decode(b);
	}
	
	public ValueType add(ValueType a) throws InvalidOperationException {
		if (a instanceof IntType)
			return new LongType(((IntType) a).getVal() + val);
		else if (a instanceof LongType)
			return new LongType(((LongType) a).getVal() + val);
		else if (a instanceof FloatType)
			return new FloatType(((FloatType) a).getVal() + val);
		else if (a instanceof ByteType)
			return new LongType(((ByteType) a).getVal() + val);
		else if (a instanceof BooleanType)
			return new LongType((((BooleanType) a).getVal() ? 1 : 0) + val);
		else
			throw new InvalidOperationException("Cannot add types", this, a);
	}
	
	public ValueType multiply(ValueType a) throws InvalidOperationException {
		if (a instanceof IntType)
			return new LongType(((IntType) a).getVal() * val);
		else if (a instanceof LongType)
			return new LongType(((LongType) a).getVal() * val);
		else if (a instanceof FloatType)
			return new FloatType(((FloatType) a).getVal() * val);
		else if (a instanceof ByteType)
			return new LongType(((ByteType) a).getVal() * val);
		else if (a instanceof BooleanType)
			return new LongType(((BooleanType) a).getVal() ? val : 0);
		else
			throw new InvalidOperationException("Cannot multiply types", this, a);
	}
	
	public ValueType divide(ValueType a) throws InvalidOperationException {
		if (a instanceof IntType)
			return new LongType(val / ((IntType) a).getVal());
		else if (a instanceof LongType)
			return new LongType(val / ((LongType) a).getVal());
		else if (a instanceof FloatType)
			return new FloatType(val / ((FloatType) a).getVal());
		else if (a instanceof ByteType)
			return new LongType(val / ((ByteType) a).getVal());
		else
			throw new InvalidOperationException("Cannot divide types", this, a);
	}
	
	public boolean equals(ValueType a) throws InvalidOperationException {
		if (a instanceof IntType)
			return ((IntType) a).getVal() == val;
		else if (a instanceof LongType)
			return ((LongType) a).getVal() == val;
		else if (a instanceof FloatType)
			return ((FloatType) a).getVal() == val;
		else if (a instanceof ByteType)
			return ((ByteType) a).getVal() == val;
		else if (a instanceof BooleanType)
			return (((BooleanType) a).getVal() ? 1 : 0) == val;
		else
			throw new InvalidOperationException("Cannot equate types", this, a);
	}
	
	public boolean less(ValueType a) throws InvalidOperationException {
		if (a instanceof IntType)
			return val < ((IntType) a).getVal();
		else if (a instanceof LongType)
			return val < ((LongType) a).getVal();
		else if (a instanceof FloatType)
			return val < ((FloatType) a).getVal();
		else if (a instanceof ByteType)
			return val < ((ByteType) a).getVal();
		else if (a instanceof BooleanType)
			return val < (((BooleanType) a).getVal() ? 1 : 0);
		else
			throw new InvalidOperationException("Cannot comapre types", this, a);
	}
	
	public void _decode(DataInput b) throws IOException {
		val = b.readLong();
	}
	
	public void _encode(DataOutput b) throws IOException {
		b.writeLong(val);
	}
	
	public ValueType negate() {
		return new LongType(-val);
	}
	
	public long getVal() {
		return val;
	}
	
	public String toTokens() {
		return T_DATA_TYPE_LONG + "" + T_GROUP_OPEN + Long.toString(val) + T_GROUP_CLOSE;
	}
	
	public String toString() {
		return String.valueOf(val);
	}
	
	public Object toJDBCObject() {
		return new Long(val);
	}
}
