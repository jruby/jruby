package org.jruby.runtime.backtrace;

import org.jruby.Ruby;
import org.jruby.util.JavaNameMangler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class BacktraceData implements Serializable {
    private RubyStackTraceElement[] backtraceElements;
    private final StackTraceElement[] javaTrace;
    private final BacktraceElement[] rubyTrace;
    private final boolean fullTrace;
    private final boolean maskNative;
    private final boolean includeNonFiltered;

    private final Pattern FILTER_CLASSES = Pattern.compile("^(org\\.jruby)|(sun\\.reflect)");

    public BacktraceData(StackTraceElement[] javaTrace, BacktraceElement[] rubyTrace, boolean fullTrace, boolean maskNative, boolean includeNonFiltered) {
        this.javaTrace = javaTrace;
        this.rubyTrace = rubyTrace;
        this.fullTrace = fullTrace;
        this.includeNonFiltered = includeNonFiltered;
        this.maskNative = maskNative;
    }

    public static final BacktraceData EMPTY = new BacktraceData(
            new StackTraceElement[0],
            new BacktraceElement[0],
            false,
            false,
            false);

    public RubyStackTraceElement[] getBacktrace(Ruby runtime) {
        if (backtraceElements == null) {
            backtraceElements = constructBacktrace(runtime.getBoundMethods());
        }
        return backtraceElements;
    }

    private RubyStackTraceElement[] constructBacktrace(Map<String, Map<String, String>> boundMethods) {
        List<RubyStackTraceElement> trace = new ArrayList<RubyStackTraceElement>(javaTrace.length);

        // used for duplicating the previous Ruby frame when masking native calls
        boolean dupFrame = false;
        String dupFrameName = null;

        // a running index into the Ruby backtrace stack, incremented for each
        // interpreter frame we encounter in the Java backtrace.
        int rubyFrameIndex = rubyTrace == null ? -1 : rubyTrace.length - 1;

        // loop over all elements in the Java stack trace
        for (int i = 0; i < javaTrace.length; i++) {

            StackTraceElement element = javaTrace[i];

            // skip unnumbered frames
            int line = element.getLineNumber();
            if (line == -1) continue;

            String className = element.getClassName();
            String methodName = element.getMethodName();
            String filename = element.getFileName();

            if (filename != null) {

                // Don't process .java files
                if (!filename.endsWith(".java")) {

                    String decodedName = JavaNameMangler.decodeMethodForBacktrace(methodName);

                    if (decodedName != null) {
                        // construct Ruby trace element
                        RubyStackTraceElement rubyElement = new RubyStackTraceElement(className, decodedName, filename, line, false);

                        // add duplicate if masking native and previous frame was native (Kernel#caller)
                        if (maskNative && dupFrame) {
                            dupFrame = false;
                            trace.add(new RubyStackTraceElement(className, dupFrameName, filename, line, false));
                        }
                        trace.add(rubyElement);
                        continue;
                    }
                }
            }

            // Java-based Ruby core methods
            String rubyName = null;
            if (
                    fullTrace || // full traces show all elements
                    (rubyName = getBoundMethodName(boundMethods, className, methodName)) != null // if a bound Java impl, always show
                    ) {

                if (rubyName == null) rubyName = methodName;

                // add package to filename
                filename = packagedFilenameFromElement(filename, className);

                // mask .java frames out for e.g. Kernel#caller
                if (maskNative) {
                    // for Kernel#caller, don't show .java frames in the trace
                    dupFrame = true;
                    dupFrameName = rubyName;
                    continue;
                }

                // construct Ruby trace element
                trace.add(new RubyStackTraceElement(className, rubyName, filename, line, false));

                // if not full trace, we're done; don't check interpreted marker
                if (!fullTrace) {
                    continue;
                }
            }

            // Interpreted frames
            if (rubyFrameIndex >= 0 &&
                    FrameType.INTERPRETED_CLASSES.contains(className) &&
                    FrameType.INTERPRETED_FRAMES.containsKey(methodName)) {

                // pop interpreter frame
                BacktraceElement rubyFrame = rubyTrace[rubyFrameIndex--];

                FrameType frameType = FrameType.INTERPRETED_FRAMES.get(methodName);

                // construct Ruby trace element
                String newName = rubyFrame.method;
                switch (frameType) {
                    case METHOD: newName = rubyFrame.method; break;
                    case BLOCK: newName = "block in " + rubyFrame.method; break;
                    case CLASS: newName = "<class:" + rubyFrame.method + ">"; break;
                    case MODULE: newName = "<module:" + rubyFrame.method + ">"; break;
                    case METACLASS: newName = "singleton class"; break;
                    case ROOT: newName = "<top>"; break;
                    case EVAL: newName = "<eval>"; break;
                }
                RubyStackTraceElement rubyElement = new RubyStackTraceElement("RUBY", newName, rubyFrame.filename, rubyFrame.line + 1, false);

                // dup if masking native and previous frame was native
                if (maskNative && dupFrame) {
                    dupFrame = false;
                    trace.add(new RubyStackTraceElement(rubyElement.getClassName(), dupFrameName, rubyElement.getFileName(), rubyElement.getLineNumber(), rubyElement.isBinding()));
                }
                trace.add(rubyElement);

                continue;
            }

            // if all else fails and this is a non-JRuby element we want to include, add it
            if (includeNonFiltered && !isFilteredClass(className)) {
                trace.add(new RubyStackTraceElement(
                        className,
                        methodName,
                        packagedFilenameFromElement(filename, className),
                        line,
                        false
                ));
                continue;
            }
        }

        RubyStackTraceElement[] rubyStackTrace = new RubyStackTraceElement[trace.size()];
        return trace.toArray(rubyStackTrace);
    }

    public static String getBoundMethodName(Map<String,Map<String,String>> boundMethods, String className, String methodName) {
        Map<String, String> javaToRuby = boundMethods.get(className);

        if (javaToRuby == null) return null;

        return javaToRuby.get(methodName);
    }

    private static String packagedFilenameFromElement(String filename, String className) {
        // stick package on the beginning
        if (filename == null) {
            filename = className.replaceAll("\\.", "/");
        } else {
            int lastDot = className.lastIndexOf('.');
            if (lastDot != -1) {
                filename = className.substring(0, lastDot + 1).replaceAll("\\.", "/") + filename;
            }
        }

        return filename;
    }

    private boolean isFilteredClass(String className) {
        return FILTER_CLASSES.matcher(className).find();
    }
}
