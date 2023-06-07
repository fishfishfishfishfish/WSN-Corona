package au.edu.usyd.corona.gui.util;


import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.table.DefaultTableCellRenderer;

import au.edu.usyd.corona.server.user.User;

@SuppressWarnings("serial")
public final class FormattingUtils {
	public static final DateRenderer DATE_RENDERER = new DateRenderer();
	public static final DottedHexRenderer DOTTED_HEX_RENDERER = new DottedHexRenderer();
	public static final PercentageRenderer PERCENTAGE_RENDERER = new PercentageRenderer();
	public static final QueryStringRenderer QUERY_RENDERER = new QueryStringRenderer();
	public static final UserRenderer USER_RENDERER = new UserRenderer();
	public static final AccelerationRenderer ACCELERATION_RENDERER = new AccelerationRenderer();
	
	private static final Map<String, CoronaCellRenderer> ATTRIBUTES;
	
	static {
		ATTRIBUTES = new HashMap<String, CoronaCellRenderer>();
		ATTRIBUTES.put("NODE", DOTTED_HEX_RENDERER);
		ATTRIBUTES.put("PARENT", DOTTED_HEX_RENDERER);
		ATTRIBUTES.put("TIME", DATE_RENDERER);
		ATTRIBUTES.put("MEMORY", PERCENTAGE_RENDERER);
		ATTRIBUTES.put("BATTERY", PERCENTAGE_RENDERER);
		ATTRIBUTES.put("CPU", PERCENTAGE_RENDERER);
		ATTRIBUTES.put("X", ACCELERATION_RENDERER);
		ATTRIBUTES.put("Y", ACCELERATION_RENDERER);
		ATTRIBUTES.put("Z", ACCELERATION_RENDERER);
	}
	
	private FormattingUtils() {
		// hidden constructor
	}
	
	public static boolean hasRenderer(String attribute) {
		attribute = attribute.toUpperCase();
		if (attribute.startsWith("COUNT("))
			return false;
		else if (attribute.contains("("))
			return ATTRIBUTES.containsKey(attribute.replaceFirst(".*\\((.*)\\)", "$1"));
		else
			return ATTRIBUTES.containsKey(attribute.toUpperCase());
	}
	
	public static CoronaCellRenderer getRenderer(String attribute) {
		attribute = attribute.toUpperCase();
		if (attribute.contains("("))
			return ATTRIBUTES.get(attribute.replaceFirst(".*\\((.*)\\)", "$1"));
		else
			return ATTRIBUTES.get(attribute.toUpperCase());
	}
	
	public static String renderAttribute(String attribute, Object value) {
		if (ATTRIBUTES.containsKey(attribute))
			return ATTRIBUTES.get(attribute).convert(value);
		else
			return value.toString();
	}
	
	public static String convertAggreate(String attribute) {
		if (attribute.contains("_")) {
			String[] tmp = attribute.split("_");
			if (tmp.length == 1)
				return tmp[0] + "(*)";
			else
				return tmp[0] + "(" + tmp[1] + ")";
		}
		else
			return attribute;
	}
	
	public static abstract class CoronaCellRenderer extends DefaultTableCellRenderer.UIResource {
		public abstract String convert(Object value);
	}
	
	/**
	 * Table cell renderer for rendering a timestamp from within the system
	 * 
	 * @author Tim Dawborn
	 */
	public static class DateRenderer extends CoronaCellRenderer {
		private final DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm:ss");
		
		@Override
		public void setValue(Object value) {
			setText(convert(value));
		}
		
		@Override
		public String convert(Object value) {
			if (value == null)
				return "";
			else if (value instanceof Long)
				return formatter.format(new Date((Long) value));
			else if (value instanceof String)
				return formatter.format(new Date(Long.parseLong((String) value)));
			else
				return formatter.format(value);
		}
	}
	
	/**
	 * Table cell renderer for rendering a User in the system
	 * 
	 * @author Tim Dawborn
	 */
	public static class UserRenderer extends CoronaCellRenderer {
		@Override
		public void setValue(Object value) {
			setText(convert(value));
		}
		
		@Override
		public String convert(Object value) {
			return (value == null) ? "" : ((User) value).getUsername();
		}
	}
	
	/**
	 * Table cell renderer for rendering a SQL query String
	 * 
	 * @author Tim Dawborn
	 */
	public static class QueryStringRenderer extends CoronaCellRenderer {
		@Override
		public void setValue(Object value) {
			final String sql = convert(value);
			setToolTipText(sql);
			setText(sql);
		}
		
		@Override
		public String convert(Object value) {
			return (value == null) ? "" : value.toString().trim().replaceAll("\\s+", " ");
		}
	}
	
	/**
	 * Table cell renderer for rendering a 64 bit dottex hex IEEE address
	 * 
	 * @author Tim Dawborn
	 */
	public static class DottedHexRenderer extends CoronaCellRenderer {
		private static final char[] HEX_DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
		private static final Map<String, String> memoized = new HashMap<String, String>();
		
		static {
			memoized.put("", "");
		}
		
		@Override
		public void setValue(Object value) {
			setText(convert(value));
		}
		
		@Override
		public String convert(Object value) {
			// memoize the results
			String key = (value == null) ? "" : value.toString();
			String val = memoized.get(key);
			if (val == null) {
				long address = Long.parseLong(key);
				char[] c = new char[19];
				for (int i = 60, j = 0; i >= 0; i -= 4, j++) {
					int digit = (int) (address >> i) & 0xF;
					c[j] = HEX_DIGITS[digit];
					if ((i % 16 == 0) && (i != 0)) {
						c[++j] = '.';
					}
				}
				val = new String(c);
				memoized.put(key, val);
			}
			return val;
		}
	}
	
	/**
	 * Table cell renderer for rendering a percentage value
	 * 
	 * @author Tim Dawborn
	 */
	public static class PercentageRenderer extends CoronaCellRenderer {
		@Override
		public void setValue(Object value) {
			setHorizontalAlignment(JLabel.RIGHT);
			setText(convert(value));
		}
		
		@Override
		public String convert(Object value) {
			return (value == null) ? "" : value.toString() + " %";
		}
	}
	
	public static class AccelerationRenderer extends CoronaCellRenderer {
		@Override
		public void setValue(Object value) {
			setHorizontalAlignment(JLabel.RIGHT);
			setText(convert(value));
		}
		
		@Override
		public String convert(Object value) {
			DecimalFormat format = new DecimalFormat("0.00000");
			return (value == null) ? "0.0" : format.format(value);
		}
	}
}
