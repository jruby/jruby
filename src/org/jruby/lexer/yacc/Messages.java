package org.jruby.lexer.yacc;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.text.MessageFormat;

public final class Messages {
    private static final String BUNDLE_NAME = "org.jruby.lexer.yacc.RubyYaccLexerProperties"; //$NON-NLS-1$
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    private Messages() {
    }

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public static String getString(String key, Object arg1) {
        try {
            return MessageFormat.format(RESOURCE_BUNDLE.getString(key), new Object[] { arg1 });
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public static String getString(String key, Object arg1, Object arg2) {
        try {
            return MessageFormat.format(RESOURCE_BUNDLE.getString(key), new Object[] { arg1, arg2 });
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
}