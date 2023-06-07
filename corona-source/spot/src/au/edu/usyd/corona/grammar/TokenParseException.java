package au.edu.usyd.corona.grammar;


/**
 * This is an exception that is thrown by the parser when the syntax is not met
 * 
 * @author Raymes Khoury
 */
public class TokenParseException extends Exception {
	public TokenParseException(String message) {
		super(message);
	}
}
