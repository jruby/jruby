package org.jruby.compiler;

import java.text.*;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {

	private static final String BUNDLE_NAME =
		"org.jruby.compiler.RubyToJavaCompiler";
	//$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE =
		ResourceBundle.getBundle(BUNDLE_NAME);

	private Messages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	public static String getFormat(String key, Object arg0) {
	    return getFormat(key, new Object[] {arg0});
	}

	public static String getFormat(String key, Object arg0, Object arg1) {
	    return getFormat(key, new Object[] {arg0, arg1});
	}
	
	private static String getFormat(String key, Object[] args) {
	    try {
			return MessageFormat.format(RESOURCE_BUNDLE.getString(key), args);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}