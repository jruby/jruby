package org.jruby.util;

public final class IdUtil {
	public static boolean isConstant(String id) {
	    return Character.isUpperCase(id.charAt(0));
    }

	public static boolean isClassVariable(String id) {
	    return id.charAt(0) == '@' && id.charAt(1) == '@';
    }

	public static boolean isInstanceVariable(String id) {
	    return id.charAt(0) == '@' && id.charAt(1) != '@';
    }
    
    public static boolean isGlobal(String id) {
        return id.charAt(0) == '$';
    }
    
	public static boolean isLocal(String id) {
	    return !isGlobal(id) && !isClassVariable(id) && !isInstanceVariable(id) && !isConstant(id);
    }

	public static boolean isAttrSet(String id) {
	    return id.endsWith("=");
	}
}