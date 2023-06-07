package au.edu.usyd.corona.server.persistence.JDBCDAO;


import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class contains a mapping of Java classes to JDBC SQL types.
 * 
 * From http://java.sun.com/j2se/1.5.0/docs/guide/jdbc/getstart/mapping.html
 * 
 * @author Raymes Khoury
 * 
 */
public class JavaToJDBCTypes {
	private static final Map<Class<?>, Integer> JAVA_TO_JDBC;
	private static final Map<Integer, Class<?>> JDBC_TO_JAVA;
	
	private JavaToJDBCTypes() {
	}
	
	static {
		JAVA_TO_JDBC = new HashMap<Class<?>, Integer>();
		JAVA_TO_JDBC.put(String.class, java.sql.Types.VARCHAR);
		JAVA_TO_JDBC.put(BigDecimal.class, java.sql.Types.NUMERIC);
		JAVA_TO_JDBC.put(Boolean.class, java.sql.Types.BIT);
		JAVA_TO_JDBC.put(Byte.class, java.sql.Types.TINYINT);
		JAVA_TO_JDBC.put(Short.class, java.sql.Types.SMALLINT);
		JAVA_TO_JDBC.put(Integer.class, java.sql.Types.INTEGER);
		JAVA_TO_JDBC.put(Long.class, java.sql.Types.BIGINT);
		JAVA_TO_JDBC.put(Float.class, java.sql.Types.REAL);
		JAVA_TO_JDBC.put(Double.class, java.sql.Types.DOUBLE);
		JAVA_TO_JDBC.put(byte[].class, java.sql.Types.VARBINARY);
		JAVA_TO_JDBC.put(Date.class, java.sql.Types.DATE);
		JAVA_TO_JDBC.put(Time.class, java.sql.Types.TIME);
		JAVA_TO_JDBC.put(Timestamp.class, java.sql.Types.TIMESTAMP);
		JAVA_TO_JDBC.put(Clob.class, java.sql.Types.CLOB);
		JAVA_TO_JDBC.put(Struct.class, java.sql.Types.STRUCT);
		
		JDBC_TO_JAVA = new HashMap<Integer, Class<?>>();
		for (Entry<Class<?>, Integer> e : JAVA_TO_JDBC.entrySet())
			JDBC_TO_JAVA.put(e.getValue(), e.getKey());
		JDBC_TO_JAVA.put(java.sql.Types.BOOLEAN, Boolean.class);
	}
	
	public static int getJDBCType(Class<?> javaType) {
		return JAVA_TO_JDBC.get(javaType);
	}
	
	public static Class<?> getJavaType(int jdbcType) {
		Class<?> res = JDBC_TO_JAVA.get(jdbcType);
		return res;
	}
}
