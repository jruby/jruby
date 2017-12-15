package org.jruby.anno;

import java.util.Collection;
import java.util.Map;

/**
 * Utility methods for generating bindings at build time. Used by AnnotationBinder.
 *
 * NOTE: This class must ONLY reference classes in the org.jruby.anno package, to avoid forcing
 * a transitive dependency on any runtime JRuby classes!
 *
 * @see org.jruby.anno.AnnotationBinder
 */
public class AnnotationHelper {

    private AnnotationHelper() { /* no instances */ }

	public static void addMethodNamesToMap(Map<String, JRubyMethod> map, JRubyMethod jrubyMethod, String simpleName) {
	    addMethodNamesToMap(map, jrubyMethod, simpleName, jrubyMethod.name(), jrubyMethod.alias());
	}

    public static void addMethodNamesToMap(final Map<String, JRubyMethod> map, JRubyMethod anno, final String simpleName,
                                           final String[] names, final String[] aliases) {
        if ( names.length == 0 ) map.put(simpleName, anno);
        else {
            for ( String name : names ) map.put(name, anno);
        }

        if ( aliases.length > 0 ) {
            for ( String alias : aliases ) map.put(alias, anno);
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
     * @see org.jruby.internal.runtime.methods.CallConfiguration#getCallerCallConfigByAnno(JRubyMethod)
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
     * Given a frame and scope requirement, return the name of the appropriate CallConfiguration.
     *
     * @see org.jruby.internal.runtime.methods.CallConfiguration#getCallConfig(boolean, boolean)
     */
    public static String getCallConfigName(boolean frame, boolean scope) {
        if (frame) {
            return scope ? "FrameFullScopeFull" : "FrameFullScopeNone";
        }
        return scope ? "FrameNoneScopeFull" : "FrameNoneScopeNone";
    }
}

