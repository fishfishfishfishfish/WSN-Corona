package au.edu.usyd.corona.srdb;


import au.edu.usyd.corona.types.ValueType;

/**
 * This class represents a constant that was provided in the original query,
 * such as "<i>42</i>" in the query "<i>SELECT * FROM NETWORK WHERE light &lt;
 * 42</i>".
 */
public class ConstantExpression extends ConditionExpression {
	private final ValueType constant;
	
	public ConstantExpression(ValueType constant) {
		super(null, null);
		this.constant = constant;
	}
	
	public ValueType eval(ValueType[] row) {
		return constant;
	}
	
	public StringBuffer toTokens() {
		return new StringBuffer(constant.toTokens());
	}
}
