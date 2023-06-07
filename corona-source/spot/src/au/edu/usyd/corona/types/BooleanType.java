package au.edu.usyd.corona.types;


import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Class representing a normal boolean value in our type system.
 * 
 * @author Tim Dawborn
 * @author Raymes Khoury
 */
public class BooleanType extends AbstractValueType {
	private boolean val;
	
	public BooleanType() {
		this.val = false;
	}
	
	public BooleanType(int val) {
		this.val = (val != 0);
	}
	
	public BooleanType(boolean val) {
		this.val = val;
	}
	
	public BooleanType(DataInputStream b) {
		decode(b);
	}
	
	public ValueType add(ValueType a) throws InvalidOperationException {
		final byte v = (byte) (val ? 1 : 0);
		if (a instanceof IntType)
			return new IntType(((IntType) a).getVal() + v);
		else if (a instanceof LongType)
			return new LongType(((LongType) a).getVal() + v);
		else if (a instanceof FloatType)
			return new FloatType(((FloatType) a).getVal() + v);
		else if (a instanceof ByteType)
			return new ByteType(((ByteType) a).getVal() + v);
		else if (a instanceof BooleanType)
			return new BooleanType(((BooleanType) a).getVal() | val);
		else
			throw new InvalidOperationException("Cannot add types", this, a);
	}
	
	public ValueType multiply(ValueType a) throws InvalidOperationException {
		byte v = (byte) (val ? 1 : 0);
		if (a instanceof IntType)
			return new IntType(((IntType) a).getVal() * v);
		else if (a instanceof LongType)
			return new LongType(((LongType) a).getVal() * v);
		else if (a instanceof FloatType)
			return new FloatType(((FloatType) a).getVal() * v);
		else if (a instanceof ByteType)
			return new ByteType(((ByteType) a).getVal() * v);
		else if (a instanceof BooleanType)
			return new BooleanType(((BooleanType) a).getVal() & val);
		else
			throw new InvalidOperationException("Cannot multiply types", this, a);
	}
	
	public ValueType divide(ValueType a) throws InvalidOperationException {
		throw new InvalidOperationException("Cannot divide types", this, a);
	}
	
	public boolean equals(ValueType a) throws InvalidOperationException {
		if (a instanceof IntType)
			return (((IntType) a).getVal() != 0) == val;
		else if (a instanceof LongType)
			return (((LongType) a).getVal() != 0) == val;
		else if (a instanceof FloatType)
			return (((FloatType) a).getVal() != 0) == val;
		else if (a instanceof ByteType)
			return (((ByteType) a).getVal() != 0) == val;
		else if (a instanceof BooleanType)
			return ((BooleanType) a).getVal() == val;
		else
			throw new InvalidOperationException("Cannot equate types", this, a);
	}
	
	public boolean less(ValueType a) throws InvalidOperationException {
		byte v = (byte) (val ? 1 : 0);
		if (a instanceof IntType)
			return v < ((IntType) a).getVal();
		else if (a instanceof LongType)
			return v < ((LongType) a).getVal();
		else if (a instanceof FloatType)
			return v < ((FloatType) a).getVal();
		else if (a instanceof ByteType)
			return v < ((ByteType) a).getVal();
		else if (a instanceof BooleanType)
			return v < (((BooleanType) a).getVal() ? 1 : 0);
		else
			throw new InvalidOperationException("Cannot comapre types", this, a);
	}
	
	protected void _decode(DataInput b) throws IOException {
		val = b.readBoolean();
	}
	
	protected void _encode(DataOutput b) throws IOException {
		b.writeBoolean(val);
	}
	
	public ValueType negate() {
		return new BooleanType(!val);
	}
	
	public boolean getVal() {
		return val;
	}
	
	public String toTokens() {
		return T_DATA_TYPE_BOOLEAN + "" + T_GROUP_OPEN + (val ? "1" : "0") + T_GROUP_CLOSE;
	}
	
	public String toString() {
		return String.valueOf(val);
	}
	
	public Object toJDBCObject() {
		return new Boolean(val);
	}
}
