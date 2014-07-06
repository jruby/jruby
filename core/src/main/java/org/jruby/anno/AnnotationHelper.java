package org.jruby.anno;

import java.util.Arrays;
import java.util.Set;

/**
 * Utility methods for generating bindings at build time. Used by AnnotationBinder.
 *
 * NOTE: This class must ONLY reference classes in the org.jruby.anno package, to avoid forcing
 * a transitive dependency on any runtime JRuby classes.
 *
 * @see org.jruby.anno.AnnotationBinder
 */
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

    /**
     * Produce a CallConfiguration name that represents what *caller* methods must prepare for
     * the method with this annotation.
     *
     * @see org.jruby.internal.runtime.methods.CallConfiguration#getCallerCallConfigNameByAnno(JRubyMethod)
     */
    public static String getCallerCallConfigNameByAnno(JRubyMethod jrubyMethod) {
        boolean frame = false;
        boolean scope = false;

        for (FrameField field : jrubyMethod.reads()) {
            frame |= field.needsFrame();
            scope |= field.needsScope();
        }
        for (FrameField field : jrubyMethod.writes()) {
            frame |= field.needsFrame();
            scope |= field.needsScope();
        }
        return getCallConfigName(frame, scope);
    }

    /**
     * Produce a CallConfiguration name that represents what must be prepared around calls to
     * the method with this annotation.
     *
     * @see org.jruby.internal.runtime.methods.CallConfiguration#getCallConfigByAnno(JRubyMethod)
     */
	public static String getCallConfigNameByAnno(JRubyMethod jrubyMethod) {
        return getCallConfigName(jrubyMethod.frame(), jrubyMethod.scope());
	}

    /**
     * Given a frame and scope requirement, return the name of the appropriate CallConfiguration.
     *
     * @see org.jruby.internal.runtime.methods.CallConfiguration#getCallConfig(boolean, boolean)
     */
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

