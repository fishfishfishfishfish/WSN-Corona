package au.edu.usyd.corona.grammar;


import junit.framework.TestCase;

/**
 * @author Tim Dawborn
 */
public class TokenLexerTest extends TestCase {
	private String stream;
	private String[] tokens;
	
	private void runLexer() {
		TokenLexer lexer = new TokenLexer(stream);
		lexer.next();
		for (int i = 0; i < tokens.length; i++, lexer.next())
			assertEquals(tokens[i], lexer.currentString());
		assertFalse(lexer.hasNext());
	}
	
	public void testNumbers() {
		stream = "1024";
		tokens = new String[]{"1024"};
		runLexer();
		
		stream = Long.MAX_VALUE + " " + Long.MIN_VALUE;
		tokens = new String[]{"" + Long.MAX_VALUE, "" + Long.MIN_VALUE};
		runLexer();
		
		stream = "12.45 96 55.0";
		tokens = new String[]{"12.45", "96", "55.0"};
		runLexer();
	}
	
	public void testSimple() {
		stream = "B()";
		tokens = new String[]{"B", "(", ")"};
		runLexer();
		
		stream = "B(1029 " + Integer.MIN_VALUE + " X())";
		tokens = new String[]{"B", "(", "1029", "" + Integer.MIN_VALUE, "X", "(", ")", ")"};
		runLexer();
	}
	
	public void testMedium() {
		stream = "B(B(X(-87 2034) 2938 -1 M()) *(19 E(12)))";
		tokens = new String[]{"B", "(", "B", "(", "X", "(", "-87", "2034", ")", "2938", "-1", "M", "(", ")", ")", "*", "(", "19", "E", "(", "12", ")", ")", ")"};
		runLexer();
	}
	
}
