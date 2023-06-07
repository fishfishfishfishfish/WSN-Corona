/*
 * Copyright 2002 Sun Microsystems, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN
 * OR ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR
 * FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR
 * PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF 
 * LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE, 
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that Software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of
 * any nuclear facility.
 */
package au.edu.usyd.corona.server.persistence.JDBCDAO;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This Class loads a given XML file containing SQL statements to be executed.
 * These SQL statements can then be accessed by a key. The SQL statements may
 * contain named or unnamed paramaters that can be substituted when accessing
 * the SQL. Named paramaters perform a direct substitution of a marker in the
 * SQL (e.g. {where}) with generated SQL. Unnamed paramaters are those that are
 * represented by a '?' in the SQL and are substituted when executing a
 * PreparedStatement. They are referred to by order only. Unnamed paramaters are
 * safer however can only be used to substitute values and not partial SQL. The
 * 
 * The XML has the doctype:
 * 
 * <pre> &lt;!DOCTYPE DAOConfiguration [
 * 
 * &lt;!ELEMENT DAOConfiguration (DAOStatements+)&gt;
 * 
 * &lt;!ELEMENT DAOStatements (SQLStatement+)&gt; &lt;!ATTLIST DAOStatements
 * database CDATA #REQUIRED &gt;
 * 
 * &lt;!ELEMENT SQLStatement (SQLFragment+)&gt; &lt;!ATTLIST SQLStatement method
 * (...) #REQUIRED &gt;
 * 
 * &lt;!ELEMENT SQLFragment (#PCDATA)&gt; &lt;!ATTLIST SQLFragment
 * excludeIfNamedParamEmpty (TRUE|FALSE) &quot;FALSE&quot; &gt; ]&gt; </pre>
 * 
 * The excludeIfNamedParamEmpty property specifies to exclude a fragment of SQL
 * if any of the named paramaters that exist in the statement are empty when
 * passed in. This allows, for example, a WHERE clause to be omitted if there
 * are no conditions in the WHERE clause to be substituted. This code is adapted
 * from the Java Petstore 1.3.1_02 example, found at
 * http://java.sun.com/blueprints/code/jps131/docs/index.html
 * 
 * @author Raymes Khoury
 */
@SuppressWarnings("serial")
public class SQLLoader {
	// XML structure
	private static final String XML_DAO_STATEMENTS = "DAOStatements";
	private static final String XML_DATABASE = "database";
	private static final String XML_SQL_STATEMENT = "SQLStatement";
	private static final String XML_METHOD = "method";
	private static final String XML_SQL_FRAGMENT = "SQLFragment";
	private static final String XML_EXCLUDE_FRAGMENT = "excludeIfNamedParamEmpty";
	
	private final Map<String, SQLStatement> sqlStatements; // A map of key->SQL statement string
	
	/**
	 * Create a new SQLLoader.
	 * 
	 * @param filename The path of the XML file to load SQL statements from
	 * @param database The name of the database for which to load SQL statements.
	 * There maybe be SQL for multiple databases within the one XML file.
	 * @throws SAXException If there is an error parsing the XML
	 * @throws ParserConfigurationException If there is an error parsing the XML
	 * @throws IOException If there is an error loading the XML file
	 */
	public SQLLoader(String filename, String database) throws SAXException, ParserConfigurationException, IOException {
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		parserFactory.setValidating(true);
		parserFactory.setNamespaceAware(true);
		InputSource input = new InputSource(filename);
		
		sqlStatements = new HashMap<String, SQLStatement>();
		loadSQLStatements(parserFactory.newSAXParser(), database, input);
	}
	
	/**
	 * Exception thrown if Parsing is finished
	 */
	private static class ParsingDoneException extends SAXException {
		ParsingDoneException() {
			super("");
		}
	}
	
	/**
	 * Inner class representing a single loaded SQL statement string
	 */
	private static class SQLStatement {
		Fragment[] fragments;
		
		/**
		 * Inner class representing a single portion of an SQL statement
		 */
		static class Fragment {
			boolean excludeIfNamedParamEmpty = false;
			List<String> namedParameters = new LinkedList<String>();
			String text;
			
			@Override
			public String toString() {
				return text;
			}
		}
		
		@Override
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < fragments.length; i++) {
				buffer.append(fragments[i].toString()).append("\n\t");
			}
			return buffer.toString();
		}
	}
	
	/**
	 * Loads the SQL statements into a hash table mapping key->SQL statement
	 * 
	 * @param parser The SAXParser to read the XML
	 * @param database The name of the database for which to read SQL
	 * @param source The InputSource of the XML file
	 * @throws SAXException If there is an error parsing the XML
	 * @throws IOException If there is an error reading the file
	 */
	private void loadSQLStatements(SAXParser parser, final String database, InputSource source) throws SAXException, IOException {
		try {
			parser.parse(source, new DefaultHandler() {
				private boolean foundEntry = false;
				private String operation = null;
				List<SQLStatement.Fragment> fragments = new ArrayList<SQLStatement.Fragment>();
				SQLStatement.Fragment fragment;
				private final StringBuffer buffer = new StringBuffer();
				
				@Override
				public void startElement(String namespace, String name, String qName, Attributes attrs) throws SAXException {
					if (!foundEntry) {
						if (name.equals(XML_DAO_STATEMENTS) && attrs.getValue(XML_DATABASE).equals(database)) {
							foundEntry = true;
						}
					}
					else if (operation != null) {
						if (name.equals(XML_SQL_FRAGMENT)) {
							fragment = new SQLStatement.Fragment();
							String value = attrs.getValue(XML_EXCLUDE_FRAGMENT);
							if (value != null) {
								fragment.excludeIfNamedParamEmpty = Boolean.parseBoolean(value);
							}
							buffer.setLength(0);
						}
					}
					else {
						if (name.equals(XML_SQL_STATEMENT)) {
							operation = attrs.getValue(XML_METHOD);
							fragments.clear();
						}
					}
					return;
				}
				
				@Override
				public void characters(char[] chars, int start, int length) throws SAXException {
					if (foundEntry && operation != null) {
						buffer.append(chars, start, length);
					}
					return;
				}
				
				@Override
				public void endElement(String namespace, String name, String qName) throws SAXException {
					if (foundEntry) {
						if (name.equals(XML_DAO_STATEMENTS)) {
							foundEntry = false;
							throw new ParsingDoneException(); // Interrupt the parsing since everything has been collected
						}
						else if (name.equals(XML_SQL_STATEMENT)) {
							SQLStatement statement = new SQLStatement();
							statement.fragments = fragments.toArray(new SQLStatement.Fragment[0]);
							sqlStatements.put(operation, statement);
							operation = null;
						}
						else if (name.equals(XML_SQL_FRAGMENT)) {
							fragment.text = buffer.toString().trim();
							Pattern test = Pattern.compile("\\{[^{}]*?\\}");
							Matcher match = test.matcher(fragment.text);
							while (match.find()) {
								fragment.namedParameters.add(match.group());
							}
							fragments.add(fragment);
							fragment = null;
						}
					}
					return;
				}
				
				@Override
				public void warning(SAXParseException exception) throws SAXParseException {
					throw exception;
				}
				
				@Override
				public void error(SAXParseException exception) throws SAXException {
					throw exception;
				}
				
				@Override
				public void fatalError(SAXParseException exception) throws SAXException {
					throw exception;
				}
			});
		}
		catch (ParsingDoneException e) {
			// Ignore
		}
		return;
	}
	
	/**
	 * Returns the SQL string for a given key, performing the text substitution
	 * of namedParams before returning the string
	 * 
	 * @param sqlStatementKey The key of the SQL statement to load
	 * @param namedParams The named parameters in the SQL statement which are to
	 * be substituted
	 * @return The constructed SQL statement
	 * @throws IOException If the SQL statement corresponding to the given key
	 * cannot be found
	 */
	public synchronized String getSQLString(String sqlStatementKey, Map<String, String> namedParams) throws IOException {
		SQLStatement sqlStatement = sqlStatements.get(sqlStatementKey);
		if (sqlStatement == null)
			throw new IOException("SQL statement: " + sqlStatementKey + " not found in specified XML file");
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < sqlStatement.fragments.length; i++) {
			String currentFragmentString = sqlStatement.fragments[i].text;
			for (String param : sqlStatement.fragments[i].namedParameters) {
				if (namedParams == null)
					currentFragmentString = "";
				else if (namedParams.containsKey(param) && namedParams.get(param) != null) {
					if (sqlStatement.fragments[i].excludeIfNamedParamEmpty && namedParams.get(param).trim().equals(""))
						currentFragmentString = "";
					else
						currentFragmentString = currentFragmentString.replace(param, namedParams.get(param));
				}
				else {
					currentFragmentString = "";
				}
			}
			buffer.append(" ");
			buffer.append(currentFragmentString);
		}
		
		return buffer.toString();
	}
	
	/**
	 * Returns the SQL string for a given key
	 * 
	 * @param sqlStatementKey The key of the SQL statement to load
	 * @return The constructed SQL statement
	 * @throws IOException If the SQL statement corresponding to the given key
	 * cannot be found
	 */
	public synchronized String getSQLString(String sqlStatementKey) throws IOException {
		return getSQLString(sqlStatementKey, null);
	}
	
	/**
	 * Builds a PreparedStatement object on the given connection for the SQL of a
	 * given key, performing textual substitution of named parameters and value
	 * substitution of the unnamed parameters. If an incorrect number of unnamed
	 * parameters are passed in, an SQLException will be thrown
	 * 
	 * @param connection The connection on which to create the PreparedStatement
	 * @param sqlStatementKey The key of the SQL to load
	 * @param parameterValues An Object array of unnamed parameters to substitute
	 * into the PreparedStatement
	 * @param namedParameters The named parameters to perform textual
	 * substitution of
	 * @return A PreparedStatement of the constructed query
	 * @throws SQLException
	 * @throws IOException
	 */
	public synchronized PreparedStatement buildSQLStatement(Connection connection, String sqlStatementKey, Object[] parameterValues, Map<String, String> namedParameters) throws SQLException, IOException {
		String sql = getSQLString(sqlStatementKey, namedParameters);
		
		PreparedStatement statement = connection.prepareStatement(sql);
		if (parameterValues != null) {
			for (int i = 0; i < parameterValues.length; i++)
				statement.setObject(i + 1, parameterValues[i]);
		}
		
		return statement;
	}
	
	/**
	 * Builds a PreparedStatement object on the given connection for the SQL of a
	 * given key, performing textual substitution of named parameters and value
	 * substitution of the unnamed parameters. If an incorrect number of unnamed
	 * parameters are passed in, an SQLException will be thrown
	 * 
	 * @param connection The connection on which to create the PreparedStatement
	 * @param sqlStatementKey The key of the SQL to load
	 * @param parameterValues An Object array of unnamed parameters to substitute
	 * into the PreparedStatement substitution of
	 * @return A PreparedStatement of the constructed query
	 * @throws SQLException
	 * @throws IOException
	 */
	public synchronized PreparedStatement buildSQLStatement(Connection connection, String sqlStatementKey, Object[] parameterValues) throws SQLException, IOException {
		return buildSQLStatement(connection, sqlStatementKey, parameterValues, null);
	}
	
	/**
	 * Builds a PreparedStatement object on the given connection for the SQL of a
	 * given key, performing textual substitution of named parameters and value
	 * substitution of the unnamed parameters. If an incorrect number of unnamed
	 * parameters are passed in, an SQLException will be thrown
	 * 
	 * @param connection The connection on which to create the PreparedStatement
	 * @param sqlStatementKey The key of the SQL to load
	 * @return A PreparedStatement of the constructed query
	 * @throws SQLException
	 * @throws IOException
	 */
	public synchronized PreparedStatement buildSQLStatement(Connection connection, String sqlStatementKey) throws SQLException, IOException {
		return buildSQLStatement(connection, sqlStatementKey, null, null);
	}
	
	/**
	 * Builds a PreparedStatement object on the given connection for the SQL of a
	 * given key, performing textual substitution of named parameters and value
	 * substitution of the unnamed parameters. If an incorrect number of unnamed
	 * parameters are passed in, an SQLException will be thrown
	 * 
	 * @param connection The connection on which to create the PreparedStatement
	 * @param sqlStatementKey The key of the SQL to load
	 * @param namedParameters The named parameters to perform textual
	 * substitution of
	 * @return A PreparedStatement of the constructed query
	 * @throws SQLException
	 * @throws IOException
	 */
	public synchronized PreparedStatement buildSQLStatement(Connection connection, String sqlStatementKey, Map<String, String> namedParameters) throws SQLException, IOException {
		return buildSQLStatement(connection, sqlStatementKey, null, namedParameters);
	}
	
}
