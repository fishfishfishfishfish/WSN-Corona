package au.edu.usyd.corona.types;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

public class BooleanTypeTest extends TestCase {
	
	private void _testCreationEncode(boolean expected, BooleanType value) throws IOException {
		assertEquals(expected, value.getVal());
		
		// test the encode/decode transitivity
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1);
		DataOutput dos = new DataOutputStream(baos);
		value.encode(dos);
		assertEquals(expected, new BooleanType(new DataInputStream(new ByteArrayInputStream(baos.toByteArray()))).getVal());
	}
	
	public void testCreation() throws IOException {
		_testCreationEncode(true, new BooleanType(true));
		_testCreationEncode(false, new BooleanType(false));
		
		_testCreationEncode(false, new BooleanType(0));
		_testCreationEncode(true, new BooleanType(1));
		_testCreationEncode(true, new BooleanType(2));
		_testCreationEncode(true, new BooleanType(-1));
	}
	
	public void testAdd() throws Exception {
		// boolean addition should be logical OR
		assertEquals(new BooleanType(true), new BooleanType(true).add(new BooleanType(true)));
		assertEquals(new BooleanType(true), new BooleanType(false).add(new BooleanType(true)));
		assertEquals(new BooleanType(true), new BooleanType(true).add(new BooleanType(false)));
		assertEquals(new BooleanType(false), new BooleanType(false).add(new BooleanType(false)));
		
		assertEquals(new IntType(0), new BooleanType(false).add(new IntType(0)));
		assertEquals(new IntType(1), new BooleanType(true).add(new IntType(0)));
		assertEquals(new IntType(2), new BooleanType(false).add(new IntType(2)));
		assertEquals(new IntType(3), new BooleanType(true).add(new IntType(2)));
		
		try {
			new BooleanType(false).add(new IEEEAddressType());
			fail();
		}
		catch (InvalidOperationException e) {
		}
	}
	
	public void testMultiply() throws Exception {
		// boolean mult should be logical AND
		assertEquals(new BooleanType(true), new BooleanType(true).multiply(new BooleanType(true)));
		assertEquals(new BooleanType(false), new BooleanType(false).multiply(new BooleanType(true)));
		assertEquals(new BooleanType(false), new BooleanType(true).multiply(new BooleanType(false)));
		assertEquals(new BooleanType(false), new BooleanType(false).multiply(new BooleanType(false)));
		
		assertEquals(new IntType(0), new BooleanType(false).multiply(new IntType(0)));
		assertEquals(new IntType(0), new BooleanType(true).multiply(new IntType(0)));
		assertEquals(new IntType(0), new BooleanType(false).multiply(new IntType(2)));
		assertEquals(new IntType(2), new BooleanType(true).multiply(new IntType(2)));
		
		try {
			new BooleanType(false).multiply(new IEEEAddressType());
			fail();
		}
		catch (InvalidOperationException e) {
		}
	}
	
	public void testNegate() {
		assertEquals(new BooleanType(true), new BooleanType(false).negate());
		assertEquals(new BooleanType(false), new BooleanType(true).negate());
	}
	
}
