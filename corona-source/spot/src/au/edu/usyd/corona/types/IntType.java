package au.edu.usyd.corona.types;


import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Class representing a normal byte value (signed 32 bit) in our type system.
 * 
 * @author Tim Dawborn
 * @author Raymes Khoury
 */
public class IntType extends AbstractValueType {
	private int val;
	
	public IntType() {
		this.val = 0;
	}
	
	public IntType(int val) {
		this.val = val;
	}
	
	public IntType(DataInputStream b) {
		decode(b);
	}
	
	public ValueType add(ValueType a) throws InvalidOperationException {
		if (a instanceof IntType)
			return new IntType(((IntType) a).getVal() + val);
		else if (a instanceof LongType)
			return new LongType(((LongType) a).getVal() + val);
		else if (a instanceof FloatType)
			return new FloatType(((FloatType) a).getVal() + val);
		else if (a instanceof ByteType)
			return new IntType(((ByteType) a).getVal() + val);
		else if (a instanceof BooleanType)
			return new IntType((((BooleanType) a).getVal() ? 1 : 0) + val);
		else
			throw new InvalidOperationException("Cannot add types", this, a);
	}
	
	public ValueType multiply(ValueType a) throws InvalidOperationException {
		if (a instanceof IntType)
			return new IntType(((IntType) a).getVal() * val);
		else if (a instanceof LongType)
			return new LongType(((LongType) a).getVal() * val);
		else if (a instanceof FloatType)
			return new FloatType(((FloatType) a).getVal() * val);
		else if (a instanceof ByteType)
			return new IntType(((ByteType) a).getVal() * val);
		else if (a instanceof BooleanType)
			return new IntType(((BooleanType) a).getVal() ? val : 0);
		else
			throw new InvalidOperationException("Cannot multiply types", this, a);
	}
	
	public ValueType divide(ValueType a) throws InvalidOperationException {
		if (a instanceof IntType)
			return new IntType(val / ((IntType) a).getVal());
		else if (a instanceof LongType)
			return new LongType(val / ((LongType) a).getVal());
		else if (a instanceof FloatType)
			return new FloatType(val / ((FloatType) a).getVal());
		else if (a instanceof ByteType)
			return new IntType(val / ((ByteType) a).getVal());
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
		val = b.readInt();
	}
	
	public void _encode(DataOutput b) throws IOException {
		b.writeInt(val);
	}
	
	public ValueType negate() {
		return new IntType(-val);
	}
	
	public int getVal() {
		return val;
	}
	
	public String toTokens() {
		return T_DATA_TYPE_INT + "" + T_GROUP_OPEN + Integer.toString(val) + T_GROUP_CLOSE;
	}
	
	public String toString() {
		return String.valueOf(val);
	}
	
	public Object toJDBCObject() {
		return new Integer(val);
	}
}
