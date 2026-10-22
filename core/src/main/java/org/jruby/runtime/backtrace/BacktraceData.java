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
    private transient Ruby runtime; // for compiled-frame label lookup (null: bare names)
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
            this.runtime = runtime;
            backtraceElements = constructBacktrace(runtime.getBoundMethods());
        }
        return backtraceElements;
    }

    public final RubyStackTraceElement[] getPartialBacktrace(Ruby runtime, int level) {
        if (backtraceElements == null) {
            this.runtime = runtime;
            backtraceElements = constructBacktrace(runtime.getBoundMethods(), level);
        }
        return backtraceElements;
    }

    public final void yieldPartialBacktrace(Ruby runtime, Predicate<RubyStackTraceElement> consumer) {
        if (backtraceElements == null) {
            this.runtime = runtime;
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
                    decodedName = compiledFrameLabel(className, methodName, type, decodedName);

                    if (decodedName != null) {
                        // skip varargs frames if we just handled the method's regular frame
                        if (previousElement != null &&
                                element.getMethodName().equals(previousElement.getMethodName() + JavaNameMangler.VARARGS_MARKER)) {

                            previousElement = null;
                            continue;
                        }

                        // skip internal sources if requested
                        if (TraceType.isExcludedInternal(filename)) {
                            // if excluding internal sources, skip frame altogether
                            if (excludeInternal) continue;

                            // if masking internal sources, replace only the highest frame with most recent Ruby frame
                            if (maskNative) {
                                dupFrame = true;
                                dupFrameName = decodedName;
                                continue;
                            }

                            // otherwise, mark internal file paths with "internal:" prefix
                            filename = TraceType.maskInternalFiles(filename);
                        }

                        // construct Ruby trace element
                        RubyStackTraceElement rubyElement = new RubyStackTraceElement(className, decodedName, filename, line, false, type);

                        // add duplicate if masking native and previous frame was native (Kernel#caller)
                        if (maskNative && dupFrame) {
                            dupFrame = false;
                            // the native method's own frame: a method frame, not the Ruby frame's type
                            if (!consumer.test(new RubyStackTraceElement(className, dupFrameName, filename, line, false, FrameType.METHOD))) {
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
                final String translatedName;
                switch (frameType) {
                    case METHOD: translatedName = rubyFrame.method; break;
                    case BLOCK: translatedName = rubyFrame.method; break;
                    case CLASS: translatedName = "<class:" + rubyFrame.method + '>'; break;
                    case MODULE: translatedName = "<module:" + rubyFrame.method + '>'; break;
                    case METACLASS: translatedName = "singleton class"; break;
                    case ROOT: translatedName = "<main>"; break;
                    case EVAL:
                        translatedName = rubyFrame.method == null || rubyFrame.method.isEmpty() ? "<main>" : rubyFrame.method;
                        break;
                    default: translatedName = rubyFrame.method;
                }

                // skip internal sources if requested
                filename = rubyFrame.filename;
                if (TraceType.isExcludedInternal(filename)) {
                    // if excluding internal sources, skip frame altogether
                    if (excludeInternal) continue;

                    // if masking internal sources, replace only the highest frame with most recent Ruby frame
                    if (maskNative) {
                        dupFrame = true;
                        dupFrameName = translatedName;
                        continue;
                    }

                    // otherwise, mark internal file paths with "internal:" prefix
                    filename = TraceType.maskInternalFiles(filename);
                }

                RubyStackTraceElement rubyElement = new RubyStackTraceElement("RUBY", translatedName, filename, rubyFrame.line + 1, false, frameType);

                // dup if masking native and previous frame was native
                if (maskNative && dupFrame) {
                    dupFrame = false;
                    // for full trace we will have two levels for interpreter, so check termination condition
                    // the masked native method's frame is a method frame; giving it the
                    // Ruby frame's type labeled e.g. sleep in a block "block in sleep"
                    if (!consumer.test(new RubyStackTraceElement(rubyElement.getClassName(), dupFrameName, rubyElement.getFileName(), rubyElement.getLineNumber(), rubyElement.isBinding(), FrameType.METHOD))) {
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

    /**
     * A compiled frame's Ruby 3.4+ label, from its scope (Ruby#compiledFrameScope)
     * when the label was only known after the frame was generated; else decoded.
     */
    private String compiledFrameLabel(String className, String methodName, FrameType type, String decoded) {
        if (runtime == null || (type != FrameType.METHOD && type != FrameType.BLOCK && type != FrameType.ROOT)) return decoded;
        org.jruby.ir.IRScope scope = runtime.compiledFrameScope(className, methodName);
        if (scope == null) return decoded;
        if (type == FrameType.ROOT) {
            return scope instanceof org.jruby.ir.IRScriptBody script ? script.getTopLabel() : decoded;
        }
        if (type == FrameType.METHOD) {
            if (scope instanceof org.jruby.ir.IRMethod m && m.getDefinedFrom() != null) {
                return m.getDefinedFrom().getBlockBacktraceLabel(scope.getId());
            }
            String label = scope.getBacktraceLabel();
            return label != null ? label : decoded;
        }
        org.jruby.ir.IRScope top = scope.getNearestTopLocalVariableScope();
        String fallback = top instanceof org.jruby.ir.IRScriptBody ? org.jruby.ir.interpreter.Interpreter.ROOT
                : top != null ? top.getId() : decoded;
        return scope.getBlockFrameName(fallback);
    }

    /**
     * The label of the innermost Ruby frame on this thread's Java stack, for a
     * Binding (and so an eval frame, which takes its label from the binding).
     * Compiled Ruby code doesn't push backtrace elements, so the current one
     * belongs to some older interpreted frame -- e.g. an eval in a compiled
     * method was labeled with a stale or bare name (and binding.source_location
     * was stale). Returns null when the
     * innermost Ruby frame is interpreted (its backtrace element is current).
     */
    public static RubyStackTraceElement currentCompiledFrame(Ruby runtime) {
        return org.jruby.runtime.ThreadContext.WALKER.walk(frames -> {
            Iterator<StackWalker.StackFrame> it = frames.iterator();
            while (it.hasNext()) {
                StackWalker.StackFrame frame = it.next();
                String className = frame.getClassName();
                String methodName = frame.getMethodName();
                if (FrameType.getInterpreterFrame(className, methodName) != null) return null;

                List<String> tuple = JavaNameMangler.decodeMethodTuple(methodName);
                if (tuple == null || methodName.endsWith(JavaNameMangler.VARARGS_MARKER)) continue;

                FrameType type = JavaNameMangler.decodeFrameTypeFromMangledName(tuple.get(1));
                String decoded = JavaNameMangler.decodeMethodName(type, tuple);
                if (decoded == null) continue;

                BacktraceData data = new BacktraceData(Stream.empty(), Stream.empty(), false, false, false, false, false);
                data.runtime = runtime;
                String label = data.compiledFrameLabel(className, methodName, type, decoded);
                if (type == FrameType.BLOCK) label = RubyStackTraceElement.blockPrefix(label) + label;
                // the frame's own file and line too: the stale element's are just as wrong
                return new RubyStackTraceElement(className, label, frame.getFileName(), frame.getLineNumber(), false, type);
            }
            return null;
        });
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
