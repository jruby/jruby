package org.jruby.runtime;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;

public enum TraceType {
    /**
     * Full raw backtraces with all Java frames included.
     */
    RAW {
        public RubyStackTraceElement[] getBacktrace(ThreadContext context, boolean nativeException) {
            return ThreadContext.gatherRawBacktrace(context.runtime, Thread.currentThread().getStackTrace());
        }

        public String printBacktrace(RubyException exception) {
            return printBacktraceMRI(exception);
        }
    },

    /**
     * A backtrace with interpreted frames intact, but don't remove Java frames.
     */
    FULL {
        public RubyStackTraceElement[] getBacktrace(ThreadContext context, boolean nativeException) {
            return getBacktrace(context, nativeException, true);
        }

        public String printBacktrace(RubyException exception) {
            return printBacktraceMRI(exception);
        }
    },

    /**
     * Normal Ruby-style backtrace, showing only Ruby and core class methods.
     */
    RUBY_FRAMED {
        public RubyStackTraceElement[] getBacktrace(ThreadContext context, boolean nativeException) {
            return getBacktrace(context, nativeException, false);
        }

        public String printBacktrace(RubyException exception) {
            return printBacktraceMRI(exception);
        }
    },

    /**
     * Normal backtrace with Ruby and core methods, but Rubinius-style rendering.
     */
    RUBINIUS {
        public RubyStackTraceElement[] getBacktrace(ThreadContext context, boolean nativeException) {
            return getBacktrace(context, nativeException, false);
        }

        public String printBacktrace(RubyException exception) {
            return printBacktraceRubinius(exception);
        }
    };

    public abstract RubyStackTraceElement[] getBacktrace(ThreadContext context, boolean nativeException);
    public abstract String printBacktrace(RubyException exception);

    protected static String printBacktraceMRI(RubyException exception) {
        Ruby runtime = exception.getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        IRubyObject backtrace = exception.callMethod(context, "backtrace");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream errorStream = new PrintStream(baos);
        boolean printedPosition = false;
        if (backtrace.isNil() || !(backtrace instanceof RubyArray)) {
            if (context.getFile() != null && context.getFile().length() > 0) {
                errorStream.print(context.getFile() + ":" + context.getLine());
                printedPosition = true;
            } else {
                errorStream.print(context.getLine());
                printedPosition = true;
            }
        } else if (((RubyArray) backtrace).getLength() == 0) {
            printErrorPos(context, errorStream);
        } else {
            IRubyObject mesg = ((RubyArray) backtrace).first();

            if (mesg.isNil()) {
                printErrorPos(context, errorStream);
            } else {
                errorStream.print(mesg);
                printedPosition = true;
            }
        }

        RubyClass type = exception.getMetaClass();
        String info = exception.toString();

        if (printedPosition) errorStream.print(": ");

        if (type == runtime.getRuntimeError() && (info == null || info.length() == 0)) {
            errorStream.print(": unhandled exception\n");
        } else {
            String path = type.getName();

            if (info.length() == 0) {
                errorStream.print(path + '\n');
            } else {
                if (path.startsWith("#")) {
                    path = null;
                }

                String tail = null;
                if (info.indexOf("\n") != -1) {
                    tail = info.substring(info.indexOf("\n") + 1);
                    info = info.substring(0, info.indexOf("\n"));
                }

                errorStream.print(info);

                if (path != null) {
                    errorStream.print(" (" + path + ")\n");
                }

                if (tail != null) {
                    errorStream.print(tail + '\n');
                }
            }
        }

        exception.printBacktrace(errorStream);

        return new String(baos.toByteArray());
    }

    private static final String FIRST_COLOR = "\033[0;31m";
    private static final String KERNEL_COLOR = "\033[0;36m";
    private static final String EVAL_COLOR = "\033[0;33m";
    private static final String CLEAR_COLOR = "\033[0m";

    protected static String printBacktraceRubinius(RubyException exception) {
        Ruby runtime = exception.getRuntime();
        ThreadContext.RubyStackTraceElement[] frames = exception.getBacktraceElements();

        ArrayList firstParts = new ArrayList();
        int longestFirstPart = 0;
        for (ThreadContext.RubyStackTraceElement frame : frames) {
            String firstPart = frame.getClassName() + "#" + frame.getMethodName();
            if (firstPart.length() > longestFirstPart) longestFirstPart = firstPart.length();
            firstParts.add(firstPart);
        }

        // determine spacing
        int center = longestFirstPart
                + 2 // initial spaces
                + 1; // spaces before "at"

        StringBuilder buffer = new StringBuilder();

        buffer
                .append("An exception has occurred:\n")
                .append("    ");

        if (exception.getMetaClass() == runtime.getRuntimeError() && exception.message(runtime.getCurrentContext()).toString().length() == 0) {
            buffer.append("No current exception (RuntimeError)");
        } else {
            buffer.append(exception.message(runtime.getCurrentContext()).toString());
        }

        buffer
                .append('\n')
                .append('\n')
                .append("Backtrace:\n");

        int i = 0;
        for (ThreadContext.RubyStackTraceElement frame : frames) {
            String firstPart = (String)firstParts.get(i);
            String secondPart = frame.getFileName() + ":" + frame.getLineNumber();

            if (i == 0) {
                buffer.append(FIRST_COLOR);
            } else if (frame.isBinding() || frame.getFileName().equals("(eval)")) {
                buffer.append(EVAL_COLOR);
            } else if (frame.getFileName().indexOf(".java") != -1) {
                buffer.append(KERNEL_COLOR);
            }
            buffer.append("  ");
            for (int j = 0; j < center - firstPart.length(); j++) {
                buffer.append(' ');
            }
            buffer.append(firstPart);
            buffer.append(" at ");
            buffer.append(secondPart);
            buffer.append(CLEAR_COLOR);
            buffer.append('\n');
            i++;
        }

        return buffer.toString();
    }

    public static IRubyObject generateMRIBacktrace(Ruby runtime, RubyStackTraceElement[] trace) {
        if (trace == null) {
            return runtime.getNil();
        }

        RubyArray traceArray = RubyArray.newArray(runtime);

        for (int i = 0; i < trace.length; i++) {
            RubyStackTraceElement element = trace[i];

            RubyString str = RubyString.newString(runtime, element.getFileName() + ":" + element.getLineNumber() + ":in `" + element.getMethodName() + "'");
            traceArray.append(str);
        }

        return traceArray;
    }

    protected static RubyStackTraceElement[] getBacktrace(ThreadContext context, boolean nativeException, boolean full) {
          return ThreadContext.gatherHybridBacktrace(
                        context.getRuntime(),
                        context.createBacktrace2(0, nativeException),
                        Thread.currentThread().getStackTrace(),
                        full);
    }

    private static void printErrorPos(ThreadContext context, PrintStream errorStream) {
        if (context.getFile() != null && context.getFile().length() > 0) {
            if (context.getFrameName() != null) {
                errorStream.print(context.getFile() + ":" + context.getLine());
                errorStream.print(":in '" + context.getFrameName() + '\'');
            } else if (context.getLine() != 0) {
                errorStream.print(context.getFile() + ":" + context.getLine());
            } else {
                errorStream.print(context.getFile());
            }
        }
    }

    public static TraceType traceTypeFor(String style) {
        if (style.equalsIgnoreCase("raw")) return TraceType.RAW;
        else if (style.equalsIgnoreCase("ruby_framed")) return TraceType.RUBY_FRAMED;
        else if (style.equalsIgnoreCase("rubinius")) return TraceType.RUBINIUS;
        else if (style.equalsIgnoreCase("full")) return TraceType.FULL;
        else return TraceType.RUBY_FRAMED;
    }
}