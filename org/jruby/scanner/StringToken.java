package org.jruby.scanner;

public class StringToken extends DefaultToken {
	private String value = null;
	
	public StringToken(int type, String value) {
		super(type);
		this.value = value;
	}
	/**
	 * Gets the value
	 * @return Returns a String
	 */
	public String getValue() {
		return value;
	}
    /**
     * Sets the value
     * @param value The value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    public String toString() {
    	return "StringToken: String = \"" + value.replaceAll("\n", "\\n") + '\"';
    }
}

