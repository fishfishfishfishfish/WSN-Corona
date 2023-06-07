package au.edu.usyd.corona.util;


import java.util.Vector;

import junit.framework.TestCase;

/**
 * @author Tim Dawborn
 */
@SuppressWarnings("unchecked")
public class CollectionToolsTest extends TestCase {
	public void testVectorToArray1() {
		Vector v = new Vector();
		for (int i = 0; i < 100; ++i)
			v.add(new Long(i));
		
		long[] actual = CollectionTools.vectorToArrayL(v);
		Object[] expected = v.toArray();
		
		assertEquals(expected.length, actual.length);
		for (int i = 0; i < expected.length; ++i)
			assertEquals(((Long) expected[i]).longValue(), actual[i]);
	}
	
	public void testVectorToArray2() {
		try {
			Vector v = new Vector();
			v.add(null);
			CollectionTools.vectorToArrayL(v);
			fail();
		}
		catch (NullPointerException e) {
		}
	}
	
	public void testVectorToArray3() {
		try {
			Vector v = new Vector();
			v.add("Hello world");
			CollectionTools.vectorToArrayL(v);
			fail();
		}
		catch (ClassCastException e) {
		}
	}
	
	public void testVectorToObjArray1() {
		Vector v = new Vector();
		
		Object[] actual = CollectionTools.vectorToArrayO(v);
		Object[] expected = v.toArray();
		
		assertEquals(expected.length, actual.length);
	}
	
	public void testVectorToObjArray2() {
		Vector v = new Vector();
		v.add("Once upon a time");
		v.add(new Long(235354634L));
		v.add(new CollectionToolsTest());
		
		Object[] actual = CollectionTools.vectorToArrayO(v);
		Object[] expected = v.toArray();
		
		assertEquals(expected.length, actual.length);
		for (int i = 0; i != actual.length; ++i)
			assertEquals(expected[i], actual[i]);
	}
	
}
