package au.edu.usyd.corona.server.persistence.JDBCDAO;


import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteTableResultsInterface;
import au.edu.usyd.corona.server.session.ResultRetrieveException;
import au.edu.usyd.corona.server.util.SQLExtractor;

/**
 * Implementation of RemoteTableResultsInterface for JDBC databases
 * 
 * @author Raymes Khoury
 * 
 */
@SuppressWarnings("serial")
public class RemoteJDBCTableResults extends UnicastRemoteObject implements RemoteTableResultsInterface {
	public static final String NAMED_PARAM_TABLE = "{table}";
	public static final String NAMED_PARAM_ATTRIBUTES = "{attribs}";
	public static final String NAMED_PARAM_VALUES = "{values}";
	public static final String NAMED_PARAM_SELECT = "{select}";
	public static final String NAMED_PARAM_WHERE = "{where}";
	public static final String NAMED_PARAM_GROUP_BY = "{groupby}";
	public static final String NAMED_PARAM_HAVING = "{having}";
	public static final String NAMED_PARAM_ORDER_BY = "{orderby}";
	
	public static final String RETRIEVE_RESULT_TABLE_KEY = "RETRIEVE_RESULT_TABLE";
	public static final String RETRIEVE_RESULT_TABLE_LIMIT_KEY = "RETRIEVE_RESULT_TABLE_LIMIT";
	
	private final DataSource datasource;
	private String countTable;
	private String retrieveRows;
	
	private final boolean tableExists;
	private Class<?>[] attributes;
	private String[] columnNames;
	
	private final SQLLoader sqlStatements;
	private final SQLExtractor sqlExtractor;
	
	public RemoteJDBCTableResults(SQLLoader sqlStatements, DataSource datasource, String query) throws DAOException, RemoteException {
		this.sqlExtractor = new SQLExtractor(query, SQLExtractor.Type.FULL_QUERY);
		this.datasource = datasource;
		Connection conn = null;
		try {
			conn = datasource.getConnection();
			if (DBUtils.tableExists(sqlExtractor.extractFrom(), conn, sqlStatements))
				tableExists = true;
			else
				tableExists = false;
		}
		catch (SQLException e) {
			throw new DAOException("Could not connect to the DataSource: " + e.getMessage());
		}
		finally {
			DBUtils.closeConnection(conn);
		}
		this.sqlStatements = sqlStatements;
		
		setupAttributes();
		setupCountTableStatement();
		setupRetrieveRowsStatement();
		
	}
	
	private void setupAttributes() throws DAOException {
		Connection conn = null;
		try {
			conn = datasource.getConnection();
			if (!tableExists) {
				attributes = new Class<?>[0];
				columnNames = new String[0];
			}
			else {
				ResultSet rsColumns = null;
				DatabaseMetaData meta = conn.getMetaData();
				rsColumns = meta.getColumns(null, null, sqlExtractor.extractFrom(), null);
				ArrayList<Class<?>> types = new ArrayList<Class<?>>();
				ArrayList<String> names = new ArrayList<String>();
				while (rsColumns.next()) {
					types.add(JavaToJDBCTypes.getJavaType(rsColumns.getInt("DATA_TYPE")));
					names.add(rsColumns.getString("COLUMN_NAME"));
				}
				attributes = types.toArray(new Class<?>[0]);
				columnNames = names.toArray(new String[0]);
			}
		}
		catch (SQLException e) {
			throw new DAOException("Could not setup attributes: " + e);
		}
		finally {
			DBUtils.closeConnection(conn);
		}
	}
	
	private void setupRetrieveRowsStatement() throws DAOException {
		
		try {
			
			Map<String, String> namedParams = new HashMap<String, String>();
			namedParams.put(NAMED_PARAM_SELECT, sqlExtractor.extractSelect());
			namedParams.put(NAMED_PARAM_TABLE, sqlExtractor.extractFrom());
			namedParams.put(NAMED_PARAM_WHERE, sqlExtractor.extractWhere());
			namedParams.put(NAMED_PARAM_GROUP_BY, sqlExtractor.extractGroupBy());
			namedParams.put(NAMED_PARAM_HAVING, sqlExtractor.extractHaving());
			namedParams.put(NAMED_PARAM_ORDER_BY, sqlExtractor.extractOrderBy());
			
			if (tableExists)
				retrieveRows = sqlStatements.getSQLString(RETRIEVE_RESULT_TABLE_LIMIT_KEY, namedParams);
			else
				retrieveRows = null;
		}
		catch (SQLException e) {
			e.printStackTrace();
			throw new DAOException("Unable to retrieve the Results", e);
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new DAOException("Unable to retrieve the Results", e);
		}
	}
	
	private void setupCountTableStatement() throws DAOException {
		
		Map<String, String> namedParams = new HashMap<String, String>();
		try {
			namedParams.put(NAMED_PARAM_SELECT, sqlExtractor.extractSelect());
			namedParams.put(NAMED_PARAM_TABLE, sqlExtractor.extractFrom());
			namedParams.put(NAMED_PARAM_WHERE, sqlExtractor.extractWhere());
			namedParams.put(NAMED_PARAM_GROUP_BY, sqlExtractor.extractGroupBy());
			namedParams.put(NAMED_PARAM_HAVING, sqlExtractor.extractHaving());
		}
		catch (SQLException e) {
			throw new DAOException("Could not parse the SQL", e);
		}
		
		try {
			countTable = sqlStatements.getSQLString(RETRIEVE_RESULT_TABLE_KEY, namedParams);
		}
		catch (IOException e) {
			throw new DAOException("Could not retrieve the number of rows in the given table", e);
		}
	}
	
	public int getNumItems() throws ResultRetrieveException {
		Connection conn = null;
		int res = 0;
		try {
			conn = datasource.getConnection();
			if (!tableExists) {
				res = 0;
			}
			else {
				res = DBUtils.countTable(countTable, conn, sqlStatements);
			}
		}
		catch (SQLException e) {
			throw new ResultRetrieveException("Could not retrieve the number of rows in the given table: " + e);
		}
		catch (DAOException e) {
			throw new ResultRetrieveException("Could not retrieve the number of rows in the given table: " + e);
		}
		finally {
			DBUtils.closeConnection(conn);
		}
		return res;
	}
	
	public List<Object[]> getItems(int startRow, int endRow) throws ResultRetrieveException {
		if (startRow < 0)
			startRow = 0;
		
		if (endRow < 0)
			endRow = 0;
		
		// Some databases return all rows if startRow == endRow, so we extract 1 row 
		if (endRow <= startRow) {
			return new ArrayList<Object[]>();
		}
		
		Connection conn = null;
		PreparedStatement retrieve = null;
		ResultSet rs = null;
		ArrayList<Object[]> results = new ArrayList<Object[]>();
		try {
			conn = datasource.getConnection();
			retrieve = conn.prepareStatement(retrieveRows);
			retrieve.setInt(1, startRow);
			retrieve.setInt(2, endRow - startRow);
			// If the table doesn't exist, return empty table
			if (!tableExists)
				return new ArrayList<Object[]>();
			
			rs = retrieve.executeQuery();
			while (rs.next()) {
				Object[] row = new Object[rs.getMetaData().getColumnCount()];
				for (int i = 0; i < row.length; i++) {
					row[i] = rs.getObject(i + 1);
				}
				results.add(row);
			}
			
		}
		catch (SQLException e) {
			e.printStackTrace();
			throw new ResultRetrieveException("Unable to retrieve the Results: " + e);
		}
		finally {
			DBUtils.closeResultSet(rs);
			DBUtils.closeStatement(retrieve);
			DBUtils.closeConnection(conn);
		}
		
		return results;
	}
	
	public synchronized int getNumCols() {
		return columnNames.length;
	}
	
	public Class<?>[] getAttributes() {
		return attributes;
	}
	
	public String[] getColumnNames() {
		return columnNames;
	}
	
}
