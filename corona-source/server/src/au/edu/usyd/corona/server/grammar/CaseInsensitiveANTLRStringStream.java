package au.edu.usyd.corona.server.grammar;


import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;

/**
 * This class helps ANTLR to ignore case in the grammar
 * 
 * Code from http://www.antlr.org/wiki/pages/viewpage.action?pageId=1782
 */
public class CaseInsensitiveANTLRStringStream extends ANTLRStringStream {
	public CaseInsensitiveANTLRStringStream(String fileName) {
		super(fileName);
	}
	
	@Override
	public int LA(int i) {
		if (i == 0)
			return 0; // undefined
		else if (i < 0)
			i++; // e.g., translate LA(-1) to use offset 0
		if ((p + i - 1) >= n)
			return CharStream.EOF;
		return Character.toUpperCase(data[p + i - 1]);
	}
	
}
