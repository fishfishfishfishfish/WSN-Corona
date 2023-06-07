package au.edu.usyd.corona.util;


import junit.framework.TestCase;

/**
 * @author Tim Dawborn
 */
public class StringToolsTest extends TestCase {
	private String expected;
	private String actual;
	private String[] expecteda;
	private String[] actuala;
	
	private final void assertArrayEquals() {
		assertTrue(expecteda.length == actuala.length);
		for (int i = 0; i != expecteda.length; ++i)
			assertEquals(expecteda[i], actuala[i]);
	}
	
	public void testExplodeC() {
		expecteda = new String[]{""};
		actuala = StringTools.explode("", ' ');
		assertArrayEquals();
		
		expecteda = new String[]{""};
		actuala = StringTools.explode(" ", ' ');
		assertArrayEquals();
		
		expecteda = new String[]{"hello", "worlD"};
		actuala = StringTools.explode("hello    worlD  ", ' ');
		assertArrayEquals();
		
		expecteda = new String[]{"", "he ", "ll o \n   ", "worlD"};
		actuala = StringTools.explode("\the \tll o \n   \tworlD\t", '\t');
		assertArrayEquals();
		
		expecteda = new String[]{"he", "o    wor", "D"};
		actuala = StringTools.explode("hello    worlD", 'l');
		assertArrayEquals();
		
		expecteda = new String[]{"", "he big fa", " ca", " sa", " on ", "he ma"};
		actuala = StringTools.explode("the big fat cat sat on the mat", 't');
		assertArrayEquals();
	}
	
	public void testExplodeS() {
		expecteda = new String[]{""};
		actuala = StringTools.explode("hello", "hello");
		assertArrayEquals();
		
		expecteda = new String[]{""};
		actuala = StringTools.explode("", "hello");
		assertArrayEquals();
		
		expecteda = new String[]{"he", "o wor", "d"};
		actuala = StringTools.explode("hello world", "l");
		assertArrayEquals();
		
		expecteda = new String[]{"hello world"};
		actuala = StringTools.explode("hello world", "");
		assertArrayEquals();
		
		expecteda = new String[]{"the c", " s", " on the big f", " m"};
		actuala = StringTools.explode("the cat sat on the big fat mat", "at");
		assertArrayEquals();
	}
	
	public void testContains() {
		assertTrue(StringTools.contains("once upon a time", ' '));
		assertFalse(StringTools.contains("once upon a time", '!'));
		assertFalse(StringTools.contains("", 'M'));
		assertFalse(StringTools.contains("mmmmmmm", 'M'));
		assertFalse(StringTools.contains("mmmmmmmM", '\0'));
		assertTrue(StringTools.contains(" \r\t\n\1\0", '\0'));
	}
	
	public void testImplode() {
		expected = "  ";
		actual = StringTools.implode(new String[]{"", "", ""}, " ");
		assertEquals(expected, actual);
		
		expected = "  ";
		actual = StringTools.implode(new String[]{"", "", ""}, ' ');
		assertEquals(expected, actual);
		
		expected = "once\nupon\na\ntime\nthere\nwas\na\ntest\ncase!";
		actual = StringTools.implode(new String[]{"once", "upon", "a", "time", "there", "was", "a", "test", "case!"}, '\n');
		assertEquals(expected, actual);
		
		expected = "hello world";
		actual = StringTools.implode(new String[]{"hello world"}, "!!!!!!");
		assertEquals(expected, actual);
		
		expected = "";
		actual = StringTools.implode(new String[]{}, "blank");
		assertEquals(expected, actual);
		
		expected = "!!cat !!fish!!mooooooo!!cow!!";
		actual = StringTools.implode(new String[]{"", "cat ", "fish", "mooooooo", "cow", ""}, "!!");
		assertEquals(expected, actual);
	}
	
	public void testMultiply() {
		expected = "";
		actual = StringTools.multiply("", 10);
		assertEquals(expected, actual);
		
		expected = "";
		actual = StringTools.multiply("X", 0);
		assertEquals(expected, actual);
		
		expected = "123456123456123456123456";
		actual = StringTools.multiply("123456", 4);
		assertEquals(expected, actual);
		
		expected = "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t";
		actual = StringTools.multiply("\t", 20);
		assertEquals(expected, actual);
	}
	
	public void testReplaceAll() {
		expected = "the cat sat on the big fat mat";
		actual = StringTools.replaceAll("the c! s! on the big f! m!", '!', "at");
		assertEquals(expected, actual);
		
		expected = "";
		actual = StringTools.replaceAll("", 't', "[][]");
		assertEquals(expected, actual);
		
		expected = "";
		actual = StringTools.replaceAll("ttttt", 't', "");
		assertEquals(expected, actual);
		
		expected = "caac";
		actual = StringTools.replaceAll("cattac", 't', "");
		assertEquals(expected, actual);
	}
	
	public void testReverse() {
		expected = "";
		actual = StringTools.reverse("");
		assertEquals(expected, actual);
		
		expected = "123456789";
		actual = StringTools.reverse("987654321");
		assertEquals(expected, actual);
		
		String t = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
		expected = t;
		actual = StringTools.reverse(t);
		assertEquals(expected, actual);
	}
}
