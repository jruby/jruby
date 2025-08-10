package org.jruby.runtime.backtrace;

import com.headius.backport9.stack.StackWalker;
import org.jruby.Ruby;
import org.jruby.util.JavaNameMangler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class BacktraceData implements Serializable {

    public static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];

    private RubyStackTraceElement[] backtraceElements;
    private final Stream<StackWalker.StackFrame> stackStream;
    private final Stream<BacktraceElement> rubyTrace;
    private final boolean fullTrace;
    private final boolean rawTrace;
    private final boolean maskNative;
    private final boolean includeNonFiltered;
    private final boolean excludeInternal;

    public BacktraceData(Stream<StackWalker.StackFrame> stackStream, Stream<BacktraceElement> rubyTrace, boolean fullTrace, boolean rawTrace, boolean maskNative, boolean includeNonFiltered, boolean excludeInternal) {
        this.stackStream = stackStream;
        this.rubyTrace = rubyTrace;
        this.fullTrace = fullTrace;
        this.rawTrace = rawTrace;
        this.maskNative = maskNative;
        this.includeNonFiltered = includeNonFiltered;
        this.excludeInternal = excludeInternal;
    }

    public BacktraceData(RubyStackTraceElement[] backtraceElements) {
        this.backtraceElements = backtraceElements;
        this.stackStream = Stream.empty();
        this.rubyTrace = Stream.empty();
        this.fullTrace = false;
        this.rawTrace = false;
        this.maskNative = false;
        this.includeNonFiltered = false;
        this.excludeInternal = false;
    }

    public static final BacktraceData EMPTY = new BacktraceData(null);

    public final RubyStackTraceElement[] getBacktrace(Ruby runtime) {
        if (backtraceElements == null) {
            backtraceElements = constructBacktrace(runtime.getBoundMethods());
        }
        return backtraceElements;
    }

    public final RubyStackTraceElement[] getPartialBacktrace(Ruby runtime, int level) {
        if (backtraceElements == null) {
            backtraceElements = constructBacktrace(runtime.getBoundMethods(), level);
        }
        return backtraceElements;
    }

    public final void yieldPartialBacktrace(Ruby runtime, Predicate<RubyStackTraceElement> consumer) {
        if (backtraceElements == null) {
            eachBacktrace(runtime.getBoundMethods(), consumer);
        }
    }

    @SuppressWarnings("unchecked")
    public RubyStackTraceElement[] getBacktraceWithoutRuby() {
        return constructBacktrace(Collections.EMPTY_MAP);

    }
    private RubyStackTraceElement[] constructBacktrace(Map<String, Map<String, String>> boundMethods) {
        return constructBacktrace(boundMethods, Integer.MAX_VALUE);
    }

    private RubyStackTraceElement[] constructBacktrace(Map<String, Map<String, String>> boundMethods, int count) {
        ArrayList<RubyStackTraceElement> trace = new ArrayList<>();

        eachBacktrace(boundMethods, (elt) -> {trace.add(elt); return trace.size() < count;});

        return trace.toArray((i) -> new RubyStackTraceElement[i]);
    }

    private void eachBacktrace(Map<String, Map<String, String>> boundMethods, Predicate<RubyStackTraceElement> consumer) {
        // used for duplicating the previous Ruby frame when masking native calls
        boolean dupFrame = false;
        String dupFrameName = null;

        // loop over all elements in the Java stack trace
        Iterator<StackWalker.StackFrame> stackIter = stackStream.iterator();
        Iterator<BacktraceElement> backIter = rubyTrace.iterator();

        // the previously encountered frame, for condensing varargs and actual
        StackWalker.StackFrame previousElement = null;
        while (stackIter.hasNext()) {
            StackWalker.StackFrame element = stackIter.next();

            // skip unnumbered frames
            int line = element.getLineNumber();

            String filename = element.getFileName();
            String methodName = element.getMethodName();
            String className = element.getClassName();

            // Only rewrite non-Java files when not in "raw" mode
            if (!(rawTrace || filename == null || filename.endsWith(".java"))) {
                // skip internal sources if requested
                if (excludeInternal && TraceType.isExcludedInternal(filename)) continue;

                List<String> mangledTuple = JavaNameMangler.decodeMethodTuple(methodName);
                if (mangledTuple != null) {
                    FrameType type = JavaNameMangler.decodeFrameTypeFromMangledName(mangledTuple.get(1));
                    String decodedName = JavaNameMangler.decodeMethodName(type, mangledTuple);

                    if (decodedName != null) {
                        // skip varargs frames if we just handled the method's regular frame
                        if (previousElement != null &&
                                element.getMethodName().equals(previousElement.getMethodName() + JavaNameMangler.VARARGS_MARKER)) {

                            previousElement = null;
                            continue;
                        }

                        // mask internal file paths
                        filename = TraceType.maskInternalFiles(filename);

                        // construct Ruby trace element
                        RubyStackTraceElement rubyElement = new RubyStackTraceElement(className, decodedName, filename, line, false, type);

                        // add duplicate if masking native and previous frame was native (Kernel#caller)
                        if (maskNative && dupFrame) {
                            dupFrame = false;
                            if (!consumer.test(new RubyStackTraceElement(className, dupFrameName, filename, line, false, type))) {
                                break;
                            }
                        }
                        if (!consumer.test(rubyElement)) {
                            break;
                        }
                        previousElement = element;
                        continue;

                    }
                }
            }

            // Java-based Ruby core methods
            String rubyName = methodName; // when fullTrace == true
            if ( fullTrace || // full traces show all elements
                 ( rubyName = getBoundMethodName(boundMethods, className, methodName) ) != null ) { // if a bound Java impl, always show

                // add package to filename
                filename = packagedFilenameFromElement(filename, className);

                // mask .java frames out for e.g. Kernel#caller
                if (maskNative) {
                    // for Kernel#caller, don't show .java frames in the trace
                    dupFrame = true; dupFrameName = rubyName; continue;
                }

                // construct Ruby trace element
                if (!consumer.test(new RubyStackTraceElement(className, rubyName, filename, line, false))) {
                    break;
                }

                // if not full trace, we're done; don't check interpreted marker
                if ( ! fullTrace ) continue;
            }

            // Interpreted frames
            final FrameType frameType;
            if ( backIter.hasNext() && (frameType = FrameType.getInterpreterFrame(className, methodName)) != null ) {

                // pop interpreter frame
                BacktraceElement rubyFrame = backIter.next();

                // construct Ruby trace element
                final String newName;
                switch (frameType) {
                    case METHOD: newName = rubyFrame.method; break;
                    case BLOCK: newName = rubyFrame.method; break;
                    case CLASS: newName = "<class:" + rubyFrame.method + '>'; break;
                    case MODULE: newName = "<module:" + rubyFrame.method + '>'; break;
                    case METACLASS: newName = "singleton class"; break;
                    case ROOT: newName = "<main>"; break;
                    case EVAL:
                        newName = rubyFrame.method == null || rubyFrame.method.isEmpty() ? "<main>" : rubyFrame.method;
                        break;
                    default: newName = rubyFrame.method;
                }

                // skip internal sources if requested
                filename = rubyFrame.filename;
                if (excludeInternal && TraceType.isExcludedInternal(filename)) continue;

                // mask internal file paths
                filename = TraceType.maskInternalFiles(filename);

                RubyStackTraceElement rubyElement = new RubyStackTraceElement("RUBY", newName, filename, rubyFrame.line + 1, false, frameType);

                // dup if masking native and previous frame was native
                if (maskNative && dupFrame) {
                    dupFrame = false;
                    // for full trace we will have two levels for interpreter, so check termination condition
                    if (!consumer.test(new RubyStackTraceElement(rubyElement.getClassName(), dupFrameName, rubyElement.getFileName(), rubyElement.getLineNumber(), rubyElement.isBinding(), rubyElement.getFrameType()))) {
                        break;
                    }
                }
                // check termination for double element
                if (!consumer.test(rubyElement)) {
                    break;
                }

                continue;
            }

            // if all else fails and this is a non-JRuby element we want to include, add it
            if (includeNonFiltered && !isFilteredClass(className)) {
                filename = packagedFilenameFromElement(filename, className);
                if (!consumer.test(new RubyStackTraceElement(className, methodName, filename, line, false))) {
                    break;
                }
            }
        }
    }

    public static String getBoundMethodName(Map<String,Map<String,String>> boundMethods, String className, String methodName) {
        Map<String, String> javaToRuby = boundMethods.get(className);
        return javaToRuby == null ? null : javaToRuby.get(methodName);
    }

    private static String packagedFilenameFromElement(final String filename, final String className) {
        // stick package on the beginning
        if (filename == null) return className.replace('.', '/');

        int lastDot = className.lastIndexOf('.');
        if (lastDot == -1) return filename;

        final String pkgPath = className.substring(0, lastDot + 1).replace('.', '/');
        // in case a native exception is re-thrown we might end-up rewriting twice e.g. :
        // 1st time className = org.jruby.RubyArray filename = RubyArray.java
        // 2nd time className = org.jruby.RubyArray filename = org/jruby/RubyArray.java
        if (filename.indexOf('/') > -1 && filename.startsWith(pkgPath)) return filename;
        return pkgPath + filename;
    }

    // ^(org\\.jruby)|(sun\\.reflect)
    private static boolean isFilteredClass(final String className) {
        if ( className.startsWith("sun.reflect.") ) return true; // sun.reflect.NativeMethodAccessorImpl.invoke
        // NOTE: previously filtered "too much" (all org.jruby prefixes) hurting traces (e.g. for jruby-openssl)
        final String org_jruby_ = "org.jruby.";
        if ( className.startsWith(org_jruby_) ) {
            final int dot = className.indexOf('.', org_jruby_.length());
            if ( dot == -1 ) return false; // e.g. org.jruby.RubyArray
            final String subPackage = className.substring(org_jruby_.length(), dot);
            switch ( subPackage ) {
                case "anno" : return true;
                case "ast" : return true;
                case "exceptions" : return true;
                case "gen" : return true;
                case "ir" : return true;
                case "internal" : return true; // e.g. org.jruby.internal.runtime.methods.DynamicMethod.call
                case "java" : return true; // e.g. org.jruby.java.invokers.InstanceMethodInvoker.call
                // NOTE: if filtering javasupport is added back consider keeping some of the internals as they
                // help identify issues and probably makes sense to NOT be filtered, namely:
                // - (most if not all) classes in the package such as Java, JavaPackage, JavaUtil
                // - sub-packages such as util, binding - maybe only filter the "proxy" sub-package?
                //case "javasupport" : return true;
                case "parser" : return true;
                case "platform" : return true;
                case "runtime" : return true; // e.g.  org.jruby.runtime.callsite.CachingCallSite.cacheAndCall
                case "util" : return true;
            }
        }
        return false;
    }
}
