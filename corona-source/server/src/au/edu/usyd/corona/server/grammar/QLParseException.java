package au.edu.usyd.corona.server.grammar;


@SuppressWarnings("serial")
public class QLParseException extends Exception {
	private final String query;
	private final int index;
	
	public QLParseException(String msg, int index, String query) {
		super(msg);
		this.index = index;
		this.query = query;
	}
	
	public QLParseException(String msg) {
		super(msg);
		this.query = "";
		this.index = 0;
	}
	
	public String getQuery() {
		return query;
	}
	
	public int getIndex() {
		return index;
	}
}
