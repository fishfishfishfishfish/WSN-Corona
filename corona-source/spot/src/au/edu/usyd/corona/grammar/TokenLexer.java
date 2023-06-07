package au.edu.usyd.corona.grammar;


/**
 * A simple state machine lexer for the token grammar
 * 
 * @author Tim Dawborn
 */
class TokenLexer {
	public static final byte TOKEN_CHAR = 0; // single character
	public static final byte TOKEN_LBRACKET = 1; // (
	public static final byte TOKEN_RBRACKET = 2; // )
	public static final byte TOKEN_SPACE = 3; // singular space character only
	public static final byte TOKEN_INTEGER = 4; // integer values in general
	public static final byte TOKEN_FLOAT = 5; // floating point values in general
	public static final byte TOKEN_STRING = 6; // strings enclosed with " "
	public static final byte TOKEN_EOF = Byte.MAX_VALUE; // EOF reached on source input
	
	private final String data;
	private int start, end;
	private int _start, _end;
	private byte token;
	
	public TokenLexer(String data) {
		this.data = data;
		this.start = -1;
		this.token = TOKEN_EOF;
	}
	
	public void next() {
		start = end;
		// make sure we dont go off the end
		if (start >= data.length()) {
			start = end = data.length();
			token = TOKEN_EOF;
			return;
		}
		
		// skip whitespace
		while (data.charAt(start) == ' ')
			start++;
		
		end = start + 1;
		char first = data.charAt(start);
		
		if (first == '(') {
			token = TOKEN_LBRACKET;
			_start = start;
			_end = end;
		}
		else if (first == ')') {
			token = TOKEN_RBRACKET;
			_start = start;
			_end = end;
		}
		else if (first == ' ') {
			token = TOKEN_SPACE;
			_start = start;
			_end = end;
		}
		else if ((first >= '0' && first <= '9') || first == '-') {
			while (end < data.length() && data.charAt(end) >= '0' && data.charAt(end) <= '9')
				end++;
			if (end < data.length() && data.charAt(end) == '.') {
				end++;
				while (end < data.length() && data.charAt(end) >= '0' && data.charAt(end) <= '9')
					end++;
				token = TOKEN_FLOAT;
			}
			else {
				token = TOKEN_INTEGER;
			}
			_start = start;
			_end = end;
		}
		else if (first == '"') {
			end++;
			for (; data.charAt(end) != '"'; end++)
				;
			end++;
			token = TOKEN_STRING;
			_start = start + 1;
			_end = end - 1;
		}
		else { // assume its a char token 
			token = TOKEN_CHAR;
			_start = start;
			_end = end;
		}
	}
	
	/**
	 * @return the current token that the lexer is looking at
	 */
	public byte token() {
		return token;
	}
	
	/**
	 * @return when the current {@link #token()} is {@link #TOKEN_STRING}, this
	 * returns the actual string
	 */
	public String currentString() {
		return data.substring(_start, _end);
	}
	
	/**
	 * @return when the current {@link #token()} is {@link #TOKEN_CHAR}, this
	 * returns the actual character
	 */
	public char currentChar() {
		return data.charAt(_start);
	}
	
	/**
	 * @return when the current {@link #token()} is {@link #TOKEN_INTEGER}, this
	 * returns the actual long number
	 */
	public long currentLong() {
		return Long.parseLong(currentString());
	}
	
	/**
	 * @see #currentLong()
	 * @return {@link #currentLong()} casted to an int
	 */
	public int currentInt() {
		return Integer.parseInt(currentString());
	}
	
	/**
	 * @see #currentLong()
	 * @return {@link #currentLong()} casted to a byte
	 */
	public byte currentByte() {
		return Byte.parseByte(currentString());
	}
	
	public float currentFloat() {
		return Float.parseFloat(currentString());
	}
	
	/**
	 * @return whether or not the lexer can see a next token, or whether we have
	 * reached EOF
	 */
	public boolean hasNext() {
		return token != TOKEN_EOF;
	}
}
