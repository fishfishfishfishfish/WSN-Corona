package au.edu.usyd.corona.server.util;


import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.Vector;

import Zql.ParseException;
import Zql.ZExp;
import Zql.ZGroupBy;
import Zql.ZQuery;
import Zql.ZStatement;
import Zql.ZqlParser;

/**
 * Parses a given SQL statement and extracts the relevant components based on
 * structure. Uses ZQL.
 * 
 * To use SQLExtractor, create in instance of SQLExtractor
 * 
 * @author Edmund Tse
 */
public class SQLExtractor {
	private String input;
	private final Type type;
	private String selectString = null, fromString = null, whereString = null,
			groupByString = null, havingString = null, orderByString = null;
	
	public static enum Type {
		WHERE, ORDER_BY, FULL_QUERY
	};
	
	public SQLExtractor(String input, Type type) {
		this.input = input;
		if (input != null)
			input = input.trim();
		this.type = type;
		
		// Empty input should get empty output
		if (input == null || input.equals(""))
			selectString = fromString = whereString = groupByString = havingString = orderByString = "";
	}
	
	/**
	 * Parses a full SQL query and separates into components. This is called
	 * lazily if the SQLExtractor is for a full query and any one of the
	 * components is requested.
	 * 
	 * @throws SQLException
	 */
	protected void parseFullQuery() throws SQLException {
		// Append trailing semicolon if missing
		if (input.charAt(input.length() - 1) != ';')
			input += ";";
		
		// Parse the query and break it up into components
		ZqlParser parser = new ZqlParser();
		parser.initParser(new ByteArrayInputStream(input.getBytes()));
		ZStatement stmt;
		try {
			stmt = parser.readStatement();
		}
		catch (ParseException e) {
			throw new SQLException("Error while parsing the input query. " + e.getMessage());
		}
		catch (Zql.TokenMgrError e) {
			// Throw exception instead - this is recoverable.
			throw new SQLException("Error while parsing the input query. " + e.getMessage());
		}
		
		if (!(stmt instanceof ZQuery))
			throw new SQLException("Cannot identify the query. Ensure it is a SELECT statement. ");
		ZQuery query = (ZQuery) stmt;
		
		Vector<?> select = query.getSelect();
		selectString = select.toString();
		selectString = selectString.substring(1, selectString.length() - 1);
		
		Vector<?> from = query.getFrom();
		fromString = from.toString();
		fromString = fromString.substring(1, fromString.length() - 1);
		
		ZExp where = query.getWhere();
		whereString = where == null ? "" : where.toString();
		
		ZGroupBy groupByChunk = query.getGroupBy();
		if (groupByChunk == null) { // If there is no GROUP BY in the SQL
			groupByString = "";
			havingString = "";
		}
		else {
			Vector<?> groupBy = query.getGroupBy().getGroupBy();
			groupByString = groupBy.toString();
			groupByString = groupByString.substring(1, groupByString.length() - 1);
			
			ZExp having = query.getGroupBy().getHaving();
			havingString = having == null ? "" : having.toString();
		}
		
		Vector<?> orderBy = query.getOrderBy();
		if (orderBy == null) // If there is no ORDER BY in the SQL
			orderByString = "";
		else {
			orderByString = orderBy.toString();
			orderByString = orderByString.substring(1, orderByString.length() - 1);
		}
	}
	
	/**
	 * Extract the SELECT portion of the query
	 * 
	 * @return the expression, empty string if where expression not found.
	 * @throws SQLException
	 */
	public String extractSelect() throws SQLException {
		if (selectString != null)
			return selectString;
		
		if (type == Type.FULL_QUERY) {
			parseFullQuery();
			return selectString;
		}
		
		//		if (type != Type.SELECT) {
		selectString = "";
		return selectString;
		//		}
	}
	
	/**
	 * Extract the FROM portion of the query
	 * 
	 * @return the expression, empty string if where expression not found.
	 * @throws SQLException
	 */
	public String extractFrom() throws SQLException {
		if (fromString != null)
			return fromString;
		
		if (type == Type.FULL_QUERY) {
			parseFullQuery();
			return fromString;
		}
		
		//		if (type != Type.FROM) {
		fromString = "";
		return fromString;
		//		}
	}
	
	/**
	 * Extract the WHERE portion of the query
	 * 
	 * @return the expression, empty string if where expression not found.
	 * @throws SQLException
	 */
	public String extractWhere() throws SQLException {
		/*
		 * Implementation note: The strategy for this is to create a dummy SQL
		 * statement, slot in the where expression and parse it for structure.
		 * Based on the query structure, extract only the WHERE component of the
		 * SQL statement, discarding all other possibly dangerous parts
		 */
		if (whereString != null)
			return whereString;
		
		if (type == Type.FULL_QUERY) {
			parseFullQuery();
			return whereString;
		}
		
		if (type != Type.WHERE) {
			whereString = "";
			return whereString;
		}
		
		String sql = "SELECT a FROM b WHERE " + input + ";";
		ZqlParser parser = new ZqlParser();
		parser.initParser(new ByteArrayInputStream(sql.getBytes()));
		
		ZStatement stmt;
		try {
			stmt = parser.readStatement();
		}
		catch (ParseException e) {
			throw new SQLException("Invalid WHERE expression: " + input);
		}
		catch (Zql.TokenMgrError e) {
			// Throw exception instead - this is recoverable.
			throw new SQLException("Error while parsing the input query. " + e.getMessage());
		}
		
		if (stmt != null || stmt instanceof ZQuery)
			whereString = ((ZQuery) stmt).getWhere().toString();
		else
			whereString = "";
		
		return whereString;
	}
	
	/**
	 * Extract the GROUP BY portion of the query
	 * 
	 * @return the expression, empty string if where expression not found.
	 * @throws SQLException
	 */
	public String extractGroupBy() throws SQLException {
		if (groupByString != null)
			return groupByString;
		
		if (type == Type.FULL_QUERY) {
			parseFullQuery();
			return groupByString;
		}
		
		//		if (type != Type.GROUP_BY) {
		groupByString = "";
		return groupByString;
		//		}
	}
	
	/**
	 * Extract the HAVING portion of the query
	 * 
	 * @return the expression, empty string if where expression not found.
	 * @throws SQLException
	 */
	public String extractHaving() throws SQLException {
		if (havingString != null)
			return havingString;
		
		if (type == Type.FULL_QUERY) {
			parseFullQuery();
			return havingString;
		}
		
		//		if (type != Type.HAVING) {
		havingString = "";
		return havingString;
		//		}
	}
	
	/**
	 * Extract the ORDER BY portion of the query
	 * 
	 * @return the expression, empty string if where expression not found.
	 * @throws SQLException
	 */
	public String extractOrderBy() throws SQLException {
		/*
		 * Implementation note: The strategy for this is to create a dummy SQL
		 * statement, slot in the where expression and parse it for structure.
		 * Based on the query structure, extract only the ORDER BY component of
		 * the SQL statement, discarding all other possibly dangerous parts
		 */
		if (orderByString != null)
			return orderByString;
		
		if (type == Type.FULL_QUERY) {
			parseFullQuery();
			return orderByString;
		}
		
		if (type != Type.ORDER_BY) {
			orderByString = "";
			return orderByString;
		}
		
		String sql = "SELECT a FROM b ORDER BY " + input + ";";
		ZqlParser parser = new ZqlParser();
		parser.initParser(new ByteArrayInputStream(sql.getBytes()));
		
		ZStatement stmt;
		try {
			stmt = parser.readStatement();
		}
		catch (ParseException e) {
			throw new SQLException("Invalid ORDER BY expression: " + input);
		}
		catch (Zql.TokenMgrError e) {
			// Throw exception instead - this is recoverable.
			throw new SQLException("Error while parsing the input query. " + e.getMessage());
		}
		
		orderByString = "";
		if (stmt != null || stmt instanceof ZQuery) {
			Vector<?> orderBy = ((ZQuery) stmt).getOrderBy();
			if (orderBy.size() > 0)
				orderByString += orderBy.get(0);
			for (int i = 1; i < orderBy.size(); i++)
				orderByString += ", " + orderBy.get(i);
		}
		
		return orderByString;
	}
}
