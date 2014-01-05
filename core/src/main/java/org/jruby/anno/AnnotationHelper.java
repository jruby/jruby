package org.jruby.anno;

import java.util.Arrays;
import java.util.Set;

public class AnnotationHelper {

	public static void addMethodNamesToSet(Set<String> set, JRubyMethod jrubyMethod, String simpleName) {
	    if (jrubyMethod.name().length == 0) {
	        set.add(simpleName);
	    } else {
	        set.addAll(Arrays.asList(jrubyMethod.name()));
	    }
	
	    if (jrubyMethod.alias().length > 0) {
	        set.addAll(Arrays.asList(jrubyMethod.alias()));
	    }
	}

	public static int getArityValue(JRubyMethod anno, int actualRequired) {
	    if (anno.optional() > 0 || anno.rest()) {
	        return -(actualRequired + 1);
	    }
	    return actualRequired;
	}

	public static String getCallConfigNameByAnno(JRubyMethod anno) {
	    return getCallConfigName(anno.frame(), anno.scope());
	}

	public static String getCallConfigName(boolean frame, boolean scope) {
	    if (frame) {
	        if (scope) {
	            return "FrameFullScopeFull";
	        } else {
	            return "FrameFullScopeNone";
	        }
	    } else if (scope) {
	        return "FrameNoneScopeFull";
	    } else {
	        return "FrameNoneScopeNone";
	    }
	}
}

