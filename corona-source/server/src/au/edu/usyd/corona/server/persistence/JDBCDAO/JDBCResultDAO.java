package au.edu.usyd.corona.server.persistence.JDBCDAO;


import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteTableResultsInterface;
import au.edu.usyd.corona.server.persistence.DAOinterface.ResultDAO;
import au.edu.usyd.corona.srdb.Table;
import au.edu.usyd.corona.types.ValueType;

/**
 * A specific implementation of the ResultDAO for JDBC databases.
 * 
 * @author Raymes Khoury
 */
class JDBCResultDAO implements ResultDAO {
	public static final String CREATE_RESULT_TABLE_KEY = "CREATE_RESULT_TABLE";
	public static final String INSERT_RESULT_ROW_KEY = "INSERT_RESULT_ROW";
	public static final String RETRIEVE_RESULT_TABLE_KEY = "RETRIEVE_RESULT_TABLE";
	
	public static final String NAMED_PARAM_TABLE = "{table}";
	public static final String NAMED_PARAM_ATTRIBUTES = "{attribs}";
	public static final String NAMED_PARAM_VALUES = "{values}";
	public static final String NAMED_PARAM_SELECT = "{select}";
	public static final String NAMED_PARAM_WHERE = "{where}";
	public static final String NAMED_PARAM_GROUP_BY = "{groupby}";
	public static final String NAMED_PARAM_HAVING = "{having}";
	public static final String NAMED_PARAM_ORDER_BY = "{orderby}";
	public static final String NAMED_PARAM_START = "{start}";
	public static final String NAMED_PARAM_END = "{end}";
	public static final String NAMED_PARAM_NUM = "{num}";
	
	public static final String TABLE_PREFIX = "TABLE_";
	
	private final DataSource dataSource;
	private final SQLLoader sqlStatements;
	private final Map<Integer, List<String>> jdbcTypeToKeyword;
	
	@SuppressWarnings("unused")
	private static final Logger logger = java.util.logging.Logger.getLogger(JDBCResultDAO.class.getCanonicalName());
	
	public JDBCResultDAO(DataSource dataSource, SQLLoader sqlStatements, Map<Integer, List<String>> jdbcTypeToKeyword) {
		this.dataSource = dataSource;
		this.sqlStatements = sqlStatements;
		this.jdbcTypeToKeyword = jdbcTypeToKeyword;
	}
	
	public synchronized void delete(int tableID) throws DAOException {
		Connection conn = null;
		PreparedStatement delete = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			Map<String, String> namedParams = new HashMap<String, String>();
			namedParams.put(NAMED_PARAM_TABLE, TABLE_PREFIX + tableID);
			delete = sqlStatements.buildSQLStatement(conn, JDBCDAOFactory.DROP_TABLE_KEY, namedParams);
			delete.executeUpdate();
			conn.commit();
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly delete the result table", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly delete the result table", e);
		}
		finally {
			DBUtils.closeStatement(delete);
			DBUtils.closeConnection(conn);
		}
	}
	
	@SuppressWarnings("unchecked")
	public synchronized void insert(Table table) throws DAOException {
		StringBuffer valuesString = new StringBuffer();
		Enumeration<ValueType[]> rows = table.elements();
		if (!rows.hasMoreElements())
			return;
		
		ValueType[] firstRow = rows.nextElement();
		for (int i = 0; i < firstRow.length; i++) {
			valuesString.append("?");
			if (i + 1 < firstRow.length)
				valuesString.append(", ");
		}
		
		Map<String, String> namedParams = new HashMap<String, String>();
		namedParams.put(NAMED_PARAM_TABLE, TABLE_PREFIX + table.getTaskID().getQueryID());
		namedParams.put(NAMED_PARAM_VALUES, valuesString.toString());
		
		String insertSQL;
		try {
			insertSQL = sqlStatements.getSQLString(INSERT_RESULT_ROW_KEY, namedParams);
		}
		catch (IOException e) {
			throw new DAOException("Could not build INSERT query statement for Result table.", e);
		}
		
		Connection conn = null;
		PreparedStatement insert = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			insert = conn.prepareStatement(insertSQL);
			
			rows = table.elements();
			while (rows.hasMoreElements()) {
				ValueType[] row = rows.nextElement();
				for (int i = 0; i < row.length; i++) {
					insert.setObject(i + 1, row[i].toJDBCObject());
				}
				insert.executeUpdate();
			}
			conn.commit();
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly insert the table rows", e);
		}
		finally {
			DBUtils.closeStatement(insert);
			DBUtils.closeConnection(conn);
		}
	}
	
	public synchronized RemoteTableResultsInterface retrieve(String sql) throws DAOException, RemoteException {
		return new RemoteJDBCTableResults(sqlStatements, dataSource, sql);
	}
	
	public synchronized void create(int tableID, String[] attributeNames, Class<?>[] attributeTypes) throws DAOException {
		if (attributeNames.length <= 0)
			throw new DAOException("Table has no attributes");
		else if (attributeNames.length != attributeTypes.length)
			throw new DAOException("Number of attribute names is not the same as the number of attribute types");
		if (tableID < 0)
			throw new DAOException("Invalid TableID (" + tableID + ")");
		
		StringBuffer tableSchemaSQL = new StringBuffer();
		for (int i = 0; i < attributeNames.length; i++) {
			Class<?> t = attributeTypes[i];
			ValueType current = null;
			try {
				current = (ValueType) t.newInstance();
			}
			catch (InstantiationException e) {
				throw new DAOException("Invalid attribute type.", e);
			}
			catch (IllegalAccessException e) {
				throw new DAOException("Invalid attribute type.", e);
			}
			Object javaObject = current.toJDBCObject();
			Class<?> javaObjectClass = javaObject.getClass();
			int jdbcType = JavaToJDBCTypes.getJDBCType(javaObjectClass);
			String sqlType = jdbcTypeToKeyword.get(jdbcType).get(0);
			tableSchemaSQL.append(attributeNames[i]);
			tableSchemaSQL.append(" ");
			tableSchemaSQL.append(sqlType);
			
			if (i + 1 < attributeNames.length)
				tableSchemaSQL.append(", ");
		}
		
		String tableName = TABLE_PREFIX + tableID;
		
		Connection conn = null;
		PreparedStatement create = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			Map<String, String> namedParams = new HashMap<String, String>();
			namedParams.put(NAMED_PARAM_TABLE, tableName);
			namedParams.put(NAMED_PARAM_ATTRIBUTES, tableSchemaSQL.toString());
			
			create = sqlStatements.buildSQLStatement(conn, CREATE_RESULT_TABLE_KEY, namedParams);
			create.executeUpdate();
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly create the table", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly create the table", e);
		}
		finally {
			DBUtils.closeStatement(create);
			DBUtils.closeConnection(conn);
		}
		
	}
	
}
