package org.jruby.scanner;

import java.util.StringTokenizer;

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
    	return "StringToken: String = \"" + replaceAll(value, "\n", "\\n") + '\"';
    }

    public String replaceAll(String before, String toreplace, String replacement) {
        StringBuffer after = new StringBuffer();
        StringTokenizer st = new StringTokenizer(before, toreplace); 
        while (st.hasMoreTokens()) {
             String chunk = st.nextToken(); 
             after.append(chunk);
             if(st.hasMoreTokens()) {
                 after.append(replacement);
             }
        }
        return after.toString();
    }
}

