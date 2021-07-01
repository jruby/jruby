package org.jruby.anno;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

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

    public static void groupFrameFields(Map<Set<FrameField>, List<String>> readGroups, Map<Set<FrameField>, List<String>> writeGroups, JRubyMethod anno, String simpleName) {
        if (anno.reads().length > 0) {
            Set<FrameField> reads = new HashSet<>(Arrays.asList(anno.reads()));
            List<String> nameList = readGroups.get(reads);
            if (nameList == null) readGroups.put(reads, nameList = new ArrayList<>());
            if (anno.name().length == 0) {
                nameList.add(simpleName);
            } else {
                nameList.addAll(Arrays.asList(anno.name()));
            }
        }

        if (anno.writes().length > 0) {
            Set<FrameField> writes = new HashSet<>(Arrays.asList(anno.writes()));
            List<String> nameList = writeGroups.get(writes);
            if (nameList == null) writeGroups.put(writes, nameList = new ArrayList<>());
            if (anno.name().length == 0) {
                nameList.add(simpleName);
            } else {
                nameList.addAll(Arrays.asList(anno.name()));
            }
        }
    }

    public static void populateMethodIndex(Map<Set<FrameField>, List<String>> accessGroups, BiConsumer<Integer, String> action) {
        if (!accessGroups.isEmpty()) {
            for (Map.Entry<Set<FrameField>, List<String>> accessEntry : accessGroups.entrySet()) {
                Set<FrameField> reads = accessEntry.getKey();
                List<String> names = accessEntry.getValue();

                int bits = FrameField.pack(reads.stream().toArray(n -> new FrameField[n]));
                String namesJoined = names.stream().distinct().collect(Collectors.joining(";"));

                action.accept(bits, namesJoined);
            }
        }
    }

    public static void addSubclassNames(List<String> classAndSubs, JRubyClass classAnno) {
        for (int i = 0; i < classAnno.overrides().length; i++) {
            classAndSubs.add(classAnno.overrides()[i].getCanonicalName());
        }
    }
}

