package org.jruby.runtime.backtrace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.compiler.JITCompiler;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.JavaNameMangler;

public class BacktraceData {
    private RubyStackTraceElement[] backtraceElements;
    private final StackTraceElement[] javaTrace;
    private final BacktraceElement[] rubyTrace;
    private final boolean fullTrace;
    private final boolean maskNative;
    private final TraceType.Gather gather;

    public BacktraceData(StackTraceElement[] javaTrace, BacktraceElement[] rubyTrace, boolean fullTrace, boolean maskNative, TraceType.Gather gather) {
        this.javaTrace = javaTrace;
        this.rubyTrace = rubyTrace;
        this.fullTrace = fullTrace;
        this.maskNative = maskNative;
        this.gather = gather;
    }

    public static final BacktraceData EMPTY = new BacktraceData(
            new StackTraceElement[0],
            new BacktraceElement[0],
            false,
            false,
            TraceType.Gather.NORMAL);

    public RubyStackTraceElement[] getBacktrace(Ruby runtime) {
        if (backtraceElements == null) {
            backtraceElements = transformBacktrace(runtime.getBoundMethods());
        }
        return backtraceElements;
    }

    private RubyStackTraceElement[] transformBacktrace(Map<String, String> boundMethods) {
        List<RubyStackTraceElement> trace = new ArrayList<RubyStackTraceElement>(javaTrace.length);
        // used for duplicating the previous Ruby frame when masking native calls
        boolean dupFrame = false;
        String dupFrameName = null;
        // a running index into the Ruby backtrace stack, incremented for each
        // interpreter frame we encounter in the Java backtrace.
        int rubyFrameIndex = rubyTrace == null ? -1 : rubyTrace.length - 1;
        // no Java trace, can't generate hybrid trace
        // TODO: Perhaps just generate the interpreter trace? Is this path ever hit?
        if (javaTrace == null) {
            return null;
        }
        for (int i = 0; i < javaTrace.length; i++) {
            StackTraceElement element = javaTrace[i];
            if (
                    element.getFileName() != null &&
                    (element.getFileName().endsWith(".rb")
                        || element.getFileName().equals("-e")
                        || element.getClassName().startsWith(JITCompiler.RUBY_JIT_PREFIX + ".")
                        || element.getMethodName().contains("$RUBY$")
                        || element.getMethodName().contains("__file__"))) {
                if (element.getLineNumber() == -1) {
                    continue;
                }
                String methodName = element.getMethodName();
                String className = element.getClassName();
                // FIXME: Formalize jitted method structure so this isn't quite as hacky
                if (className.startsWith(JITCompiler.RUBY_JIT_PREFIX)) {
                    // pull out and demangle the method name
                    methodName = className.substring(JITCompiler.RUBY_JIT_PREFIX.length() + 1, className.lastIndexOf("_"));
                    methodName = JavaNameMangler.demangleMethodName(methodName);
                    RubyStackTraceElement rubyElement = new RubyStackTraceElement(className, methodName, element.getFileName(), element.getLineNumber(), false);
                    // dup if masking native and previous frame was native
                    if (maskNative && dupFrame) {
                        dupFrame = false;
                        trace.add(new RubyStackTraceElement(rubyElement.getClassName(), dupFrameName, rubyElement.getFileName(), rubyElement.getLineNumber(), rubyElement.isBinding()));
                    }
                    trace.add(rubyElement);
                    // if it's a synthetic call, use it but gobble up parent calls
                    // TODO: need to formalize this better
                    if (element.getMethodName().contains("$RUBY$SYNTHETIC")) {
                        // gobble up at least one parent, and keep going if there's more synthetic frames
                        while (element.getMethodName().indexOf("$RUBY$SYNTHETIC") != -1 && ++i < javaTrace.length) {
                            element = javaTrace[i];
                        }
                    }
                    continue;
                }
                int RUBYindex = methodName.indexOf("$RUBY$");
                if (RUBYindex >= 0) {
                    // if it's a synthetic call, use it but gobble up parent calls
                    // TODO: need to formalize this better
                    methodName = methodName.substring(RUBYindex);
                    if (methodName.startsWith("$RUBY$SYNTHETIC")) {
                        methodName = methodName.substring("$RUBY$SYNTHETIC".length());
                        methodName = JavaNameMangler.demangleMethodName(methodName);
                        if (methodName.equals("__file__")) {
                            methodName = "(root)";
                        }
                        RubyStackTraceElement rubyElement = new RubyStackTraceElement(className, methodName, element.getFileName(), element.getLineNumber(), false);
                        // dup if masking native and previous frame was native
                        if (maskNative && dupFrame) {
                            dupFrame = false;
                            trace.add(new RubyStackTraceElement(rubyElement.getClassName(), dupFrameName, rubyElement.getFileName(), rubyElement.getLineNumber(), rubyElement.isBinding()));
                        }
                        trace.add(rubyElement);
                        // gobble up at least one parent, and keep going if there's more synthetic frames
                        while (element.getMethodName().indexOf("$RUBY$SYNTHETIC") != -1 && ++i < javaTrace.length) {
                            element = javaTrace[i];
                        }
                        continue;
                    }
                    methodName = methodName.substring("$RUBY$".length());
                    methodName = JavaNameMangler.demangleMethodName(methodName);
                    RubyStackTraceElement rubyElement = new RubyStackTraceElement(className, methodName, element.getFileName(), element.getLineNumber(), false);
                    // dup if masking native and previous frame was native
                    if (maskNative && dupFrame) {
                        dupFrame = false;
                        trace.add(new RubyStackTraceElement(rubyElement.getClassName(), dupFrameName, rubyElement.getFileName(), rubyElement.getLineNumber(), rubyElement.isBinding()));
                    }
                    trace.add(rubyElement);
                    continue;
                }
                if (methodName.equals("__file__") && !element.getFileName().endsWith("AbstractScript.java")) {
                    methodName = "(root)";
                    RubyStackTraceElement rubyElement = new RubyStackTraceElement(className, methodName, element.getFileName(), element.getLineNumber(), false);
                    // dup if masking native and previous frame was native
                    if (maskNative && dupFrame) {
                        dupFrame = false;
                        trace.add(new RubyStackTraceElement(rubyElement.getClassName(), dupFrameName, rubyElement.getFileName(), rubyElement.getLineNumber(), rubyElement.isBinding()));
                    }
                    trace.add(rubyElement);
                    continue;
                }
            }
            String dotClassMethod = element.getClassName() + "." + element.getMethodName();
            String rubyName = null;
            if (fullTrace || (rubyName = boundMethods.get(dotClassMethod)) != null) {
                String filename = element.getFileName();

                // stick package on the beginning
                if (filename == null) {
                    filename = element.getClassName().replaceAll("\\.", "/");
                } else {
                    int lastDot = element.getClassName().lastIndexOf('.');
                    if (lastDot != -1) {
                        filename = element.getClassName().substring(0, lastDot + 1).replaceAll("\\.", "/") + filename;
                    }
                }
                if (maskNative) {
                    // for Kernel#caller, don't show .java frames in the trace
                    dupFrame = true;
                    dupFrameName = rubyName;
                    continue;
                } else {
                    if (rubyName == null) rubyName = element.getMethodName();
                    trace.add(new RubyStackTraceElement(element.getClassName(), rubyName, filename, element.getLineNumber(), false));
                }
                // if not full trace, we're done; don't check interpreted marker
                if (!fullTrace) {
                    continue;
                }
            }
            String classMethod = element.getClassName() + "." + element.getMethodName();
            FrameType frameType = FrameType.INTERPRETED_FRAMES.get(classMethod);
            if (frameType != null && rubyFrameIndex >= 0) {
                // Frame matches one of our markers for "interpreted" calls
                BacktraceElement rubyFrame = rubyTrace[rubyFrameIndex];
                RubyStackTraceElement rubyElement = new RubyStackTraceElement(rubyFrame.klass, rubyFrame.method, rubyFrame.filename, rubyFrame.line + 1, false);
                // dup if masking native and previous frame was native
                if (maskNative && dupFrame) {
                    dupFrame = false;
                    trace.add(new RubyStackTraceElement(rubyElement.getClassName(), dupFrameName, rubyElement.getFileName(), rubyElement.getLineNumber(), rubyElement.isBinding()));
                }
                trace.add(rubyElement);
                rubyFrameIndex--;
                continue;
            }
        }
        RubyStackTraceElement[] rubyStackTrace = new RubyStackTraceElement[trace.size()];
        return (RubyStackTraceElement[]) trace.toArray(rubyStackTrace);
    }
}
