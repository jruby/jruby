package org.jruby.util;

import org.jruby.RubyId;

public final class IdUtil {
	public static boolean isConstant(String id) {
	    return Character.isUpperCase(id.charAt(0));
    }

	public static boolean isConstant(RubyId id) {
	    return id.isConstId();
    }
    
	public static boolean isClassVariable(String id) {
	    return id.charAt(0) == '@' && id.charAt(1) == '@';
    }

	public static boolean isInstanceVariable(String id) {
	    return id.charAt(0) == '@' && id.charAt(1) != '@';
    }
}