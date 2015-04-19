package org.jruby.runtime.backtrace;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.PrintStream;
import java.util.Arrays;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class TraceType {

    private static final Logger LOG = LoggerFactory.getLogger("TraceType");

    private final Gather gather;
    private final Format format;

    public TraceType(Gather gather, Format format) {
        this.gather = gather;
        this.format = format;
    }

    public Gather getGather() {
        return gather;
    }

    public Format getFormat() {
        return format;
    }

    /**
     * Get a normal Ruby backtrace, using the current Gather type.
     *
     * @param context
     * @param nativeException
     * @return
     */
    public BacktraceData getBacktrace(ThreadContext context, boolean nativeException) {
        return gather.getBacktraceData(context, nativeException);
    }

    /**
     * Get an integrated Ruby/Java backtrace if the current Gather type is NORMAL
     *
     * @param context
     * @param javaTrace
     * @return
     */
    public BacktraceData getIntegratedBacktrace(ThreadContext context, StackTraceElement[] javaTrace) {
        return gather.getIntegratedBacktraceData(context, javaTrace);
    }

    public String printBacktrace(RubyException exception, boolean console) {
        return format.printBacktrace(exception, console);
    }

    public static void logBacktrace(RubyStackTraceElement[] trace) {
        LOG.info("Backtrace generated:");
        for (RubyStackTraceElement element : trace) {
            LOG.info("  " + element.getFileName() + ":" + element.getLineNumber() + " in " + element.getMethodName());
        }
    }
    
    public static void dumpException(RubyException exception) {
        LOG.info("Exception raised: {} : {}", exception.getMetaClass(), exception);
    }
    
    public static void dumpBacktrace(RubyException exception) {
        Ruby runtime = exception.getRuntime();
        System.err.println("Backtrace generated:\n" + Format.JRUBY.printBacktrace(exception, runtime.getPosix().isatty(FileDescriptor.err)));
    }
    
    public static void dumpCaller(RubyArray trace) {
        LOG.info("Caller backtrace generated:\n" + trace);
    }
    
    public static void dumpCaller(RubyStackTraceElement[] trace) {
        LOG.info("Caller backtrace generated:\n" + Arrays.toString(trace));
    }

    public static void dumpWarning(RubyStackTraceElement[] trace) {
        LOG.info("Warning backtrace generated:\n" + Arrays.toString(trace));
    }

    public static TraceType traceTypeFor(String style) {
        if (style.equalsIgnoreCase("raw")) return new TraceType(Gather.RAW, Format.JRUBY);
        else if (style.equalsIgnoreCase("ruby_framed")) return new TraceType(Gather.NORMAL, Format.JRUBY);
        else if (style.equalsIgnoreCase("normal")) return new TraceType(Gather.NORMAL, Format.JRUBY);
        // deprecated, just uses jruby format now
        else if (style.equalsIgnoreCase("rubinius")) return new TraceType(Gather.NORMAL, Format.JRUBY);
        else if (style.equalsIgnoreCase("full")) return new TraceType(Gather.FULL, Format.JRUBY);
        else if (style.equalsIgnoreCase("mri")) return new TraceType(Gather.NORMAL, Format.MRI);
        else return new TraceType(Gather.NORMAL, Format.JRUBY);
    }
    
    public enum Gather {
        /**
         * Full raw backtraces with all Java frames included.
         */
        RAW {
            public BacktraceData getBacktraceData(ThreadContext context, StackTraceElement[] javaTrace, boolean nativeException) {
                return new BacktraceData(
                        javaTrace,
                        new BacktraceElement[0],
                        true,
                        false,
                        false);
            }
        },

        /**
         * A backtrace with interpreted frames intact, but don't remove Java frames.
         */
        FULL {
            public BacktraceData getBacktraceData(ThreadContext context, StackTraceElement[] javaTrace, boolean nativeException) {
        return new BacktraceData(
                        javaTrace,
                        context.createBacktrace2(0, nativeException),
                        true,
                        false,
                        false);
            }
        },

        /**
         * A normal Ruby-style backtrace, but which includes any non-org.jruby frames
         */
        INTEGRATED {
            public BacktraceData getBacktraceData(ThreadContext context, StackTraceElement[] javaTrace, boolean nativeException) {
                return new BacktraceData(
                        javaTrace,
                        context.createBacktrace2(0, nativeException),
                        false,
                        false,
                        true);
            }
        },

        /**
         * Normal Ruby-style backtrace, showing only Ruby and core class methods.
         */
        NORMAL {
            public BacktraceData getBacktraceData(ThreadContext context, StackTraceElement[] javaTrace, boolean nativeException) {
                return new BacktraceData(
                        javaTrace,
                        context.createBacktrace2(0, nativeException),
                        false,
                        context.runtime.getInstanceConfig().getBacktraceMask(),
                        false);
            }
        },

        /**
         * Normal Ruby-style backtrace, showing only Ruby and core class methods.
         */
        CALLER {
            public BacktraceData getBacktraceData(ThreadContext context, StackTraceElement[] javaTrace, boolean nativeException) {
                return new BacktraceData(
                        javaTrace,
                        context.createBacktrace2(0, nativeException),
                        false,
                        true,
                        false);
            }
        };

        /**
         * Gather backtrace data for a normal Ruby trace.
         *
         * @param context
         * @param nativeException
         * @return
         */
        public BacktraceData getBacktraceData(ThreadContext context, boolean nativeException) {
            BacktraceData data = getBacktraceData(context, Thread.currentThread().getStackTrace(), nativeException);

            context.runtime.incrementBacktraceCount();
            if (RubyInstanceConfig.LOG_BACKTRACES) logBacktrace(data.getBacktrace(context.runtime));

            return data;
        }

        /**
         * Gather backtrace data for an integrated trace if the current gather type is "NORMAL", otherwise use the
         * current gather type.
         * 
         * @param context
         * @param javaTrace
         * @return
         */
        public BacktraceData getIntegratedBacktraceData(ThreadContext context, StackTraceElement[] javaTrace) {
            Gather useGather = this;

            if (useGather == NORMAL) {
                useGather = INTEGRATED;
            }
            
            BacktraceData data = useGather.getBacktraceData(context, javaTrace, false);

            context.runtime.incrementBacktraceCount();
            if (RubyInstanceConfig.LOG_BACKTRACES) logBacktrace(data.getBacktrace(context.runtime));

            return data;
        }

        public abstract BacktraceData getBacktraceData(ThreadContext context, StackTraceElement[] javaTrace, boolean nativeException);
    }
    
    public enum Format {
        /**
         * Formatting like C Ruby
         */
        MRI {
            public String printBacktrace(RubyException exception, boolean console) {
                return printBacktraceMRI(exception, console);
            }

            public void renderBacktrace(RubyStackTraceElement[] elts, StringBuilder buffer, boolean color) {
                renderBacktraceMRI(elts, buffer, color);
            }
        },

        /**
         * New JRuby formatting
         */
        JRUBY {
            public String printBacktrace(RubyException exception, boolean console) {
                return printBacktraceJRuby(exception, console);
            }

            public void renderBacktrace(RubyStackTraceElement[] elts, StringBuilder buffer, boolean color) {
                renderBacktraceJRuby(elts, buffer, color);
            }
        };

        public abstract String printBacktrace(RubyException exception, boolean console);
        public abstract void renderBacktrace(RubyStackTraceElement[] elts, StringBuilder buffer, boolean color);
    }

    protected static String printBacktraceMRI(RubyException exception, boolean console) {
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

        exception.printBacktrace(errorStream, 1);

        return new String(baos.toByteArray());
    }

    private static final String FIRST_COLOR = "\033[0;31m";
    private static final String KERNEL_COLOR = "\033[0;36m";
    private static final String EVAL_COLOR = "\033[0;33m";
    private static final String CLEAR_COLOR = "\033[0m";

    protected static String printBacktraceJRuby(RubyException exception, boolean console) {
        Ruby runtime = exception.getRuntime();

        StringBuilder buffer = new StringBuilder();
        boolean color = console && runtime.getInstanceConfig().getBacktraceColor();

        // exception line
        String message = exception.message(runtime.getCurrentContext()).toString();
        if (exception.getMetaClass() == runtime.getRuntimeError() && message.length() == 0) {
            message = "No current exception";
        }
        buffer
                .append(exception.getMetaClass().getName())
                .append(": ")
                .append(message)
                .append('\n');

        RubyStackTraceElement[] frames = exception.getBacktraceElements();
        if (frames == null) frames = RubyStackTraceElement.EMPTY_ARRAY;
        renderBacktraceJRuby(frames, buffer, color);


        return buffer.toString();
    }

    private static void renderBacktraceJRuby(RubyStackTraceElement[] frames, StringBuilder buffer, boolean color) {
        // find longest method name
        int longestMethod = 0;
        for (RubyStackTraceElement frame : frames) {
            longestMethod = Math.max(longestMethod, frame.getMethodName().length());
        }

        // backtrace lines
        boolean first = true;
        for (RubyStackTraceElement frame : frames) {
            if (color) {
                if (first) {
                    buffer.append(FIRST_COLOR);
                } else if (frame.isBinding() || frame.getFileName().equals("(eval)")) {
                    buffer.append(EVAL_COLOR);
                } else if (frame.getFileName().indexOf(".java") != -1) {
                    buffer.append(KERNEL_COLOR);
                }
                first = false;
            }

            buffer.append("  ");

            // method name
            String methodName = frame.getMethodName();
            for (int j = 0; j < longestMethod - methodName.length(); j++) {
                buffer.append(' ');
            }
            buffer
                    .append(methodName)
                    .append(" at ")
                    .append(frame.getFileName())
                    .append(':')
                    .append(frame.getLineNumber());

            if (color) {
                buffer.append(CLEAR_COLOR);
            }

            buffer
                    .append('\n');
        }
    }

    private static void renderBacktraceMRI(RubyStackTraceElement[] trace, StringBuilder buffer, boolean color) {
        for (int i = 0; i < trace.length; i++) {
            RubyStackTraceElement element = trace[i];

            buffer
                    .append(element.getFileName())
                    .append(':')
                    .append(element.getLineNumber())
                    .append(":in `")
                    .append(element.getMethodName())
                    .append("'\n");
        }
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
}