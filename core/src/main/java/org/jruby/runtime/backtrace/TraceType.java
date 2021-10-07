package org.jruby.runtime.backtrace;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.stream.Stream;

import com.headius.backport9.stack.StackWalker;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyHash;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyString;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.jruby.util.TypeConverter;

public class TraceType {

    private static final Logger LOG = LoggerFactory.getLogger(TraceType.class);
    private static final StackWalker WALKER = ThreadContext.WALKER;
    private static final String[] FULL_MESSAGE_KEYS = {"highlight", "order"};

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
     * @return
     */
    public BacktraceData getBacktrace(ThreadContext context) {
        return gather.getBacktraceData(context);
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

    public static void logBacktrace(Ruby runtime, RubyStackTraceElement[] trace) {
        if (trace == null) trace = RubyStackTraceElement.EMPTY_ARRAY;

        final StringBuilder buffer = new StringBuilder(64 + trace.length * 48);

        buffer.append("Backtrace generated:\n");

        renderBacktraceJRuby(runtime, trace, buffer, false);

        LOG.info(buffer.toString());
    }

    public static void logException(RubyException exception) {
        LOG.info("Exception raised: {} : {}", exception.getMetaClass(), exception);
    }

    /**
     * @deprecated use {@link #logException(org.jruby.RubyException)}
     */
    public static void dumpException(RubyException exception) {
        logException(exception);
    }

    public static void dumpBacktrace(RubyException exception) {
        Ruby runtime = exception.getRuntime();
        System.err.println("Backtrace generated:\n" + printBacktraceJRuby(exception, runtime.getPosix().isatty(FileDescriptor.err)));
    }

    public static void logCaller(RubyArray trace) {
        StringBuilder buffer = new StringBuilder(64 + trace.size() * 48);

        buffer.append("Caller backtrace generated:\n");

        for (int i = 0; i < trace.size(); i++) {
            // elements are already rendered as its an Array<RubyString>
            buffer.append("  ").append(trace.eltInternal(i)).append('\n');
        }

        LOG.info(buffer.toString());
    }

    /**
     * @deprecated use {@link #logCaller(org.jruby.RubyArray)}
     */
    public static void dumpCaller(RubyArray trace) {
        logCaller(trace);
    }

    public static void logCaller(RubyStackTraceElement[] trace) {
        if (trace == null) trace = RubyStackTraceElement.EMPTY_ARRAY;

        LOG.info( formatWithMRIBacktrace("Caller backtrace generated:\n", trace).toString() );
    }

    private static StringBuilder formatWithMRIBacktrace(final String message, RubyStackTraceElement[] trace) {
        StringBuilder buffer = new StringBuilder(64 + trace.length * 48);

        buffer.append(message);

        renderBacktraceMRI(trace, "  ", buffer, false);

        return buffer;
    }

    /**
     * @deprecated use {@link #logCaller(org.jruby.runtime.backtrace.RubyStackTraceElement[]) }
     */
    public static void dumpCaller(RubyStackTraceElement[] trace) {
        logCaller(trace);
    }

    public static void logWarning(RubyStackTraceElement[] trace) {
        if (trace == null) trace = RubyStackTraceElement.EMPTY_ARRAY;

        LOG.info( formatWithMRIBacktrace("Warning backtrace generated:\n", trace).toString() );
    }

    /**
     * @deprecated use {@link #logWarning(org.jruby.runtime.backtrace.RubyStackTraceElement[])
     */
    public static void dumpWarning(RubyStackTraceElement[] trace) {
        logWarning(trace);
    }

    public static TraceType traceTypeFor(String style) {
        if (style.equalsIgnoreCase("raw")) return new TraceType(Gather.RAW, Format.JRUBY);
        else if (style.equalsIgnoreCase("ruby_framed")) return new TraceType(Gather.NORMAL, Format.JRUBY);
        else if (style.equalsIgnoreCase("normal")) return new TraceType(Gather.NORMAL, Format.JRUBY);
        else if (style.equalsIgnoreCase("full")) return new TraceType(Gather.FULL, Format.JRUBY);
        else if (style.equalsIgnoreCase("mri")) return new TraceType(Gather.NORMAL, Format.MRI);
        else return new TraceType(Gather.NORMAL, Format.JRUBY);
    }

    public enum Gather {
        /**
         * Full raw backtraces with all Java frames included.
         */
        RAW {
            public BacktraceData getBacktraceData(ThreadContext context, Stream<StackWalker.StackFrame> stackStream) {
                return new BacktraceData(
                        stackStream,
                        Stream.empty(),
                        true,
                        true,
                        false,
                        false);
            }
        },

        /**
         * A backtrace with interpreted frames intact, but don't remove Java frames.
         */
        FULL {
            public BacktraceData getBacktraceData(ThreadContext context, Stream<StackWalker.StackFrame> stackStream) {
                return new BacktraceData(
                        stackStream,
                        context.getBacktrace(),
                        true,
                        false,
                        false,
                        false);
            }
        },

        /**
         * A normal Ruby-style backtrace, but which includes any non-org.jruby frames
         */
        INTEGRATED {
            public BacktraceData getBacktraceData(ThreadContext context, Stream<StackWalker.StackFrame> stackStream) {
                return new BacktraceData(
                        stackStream,
                        context.getBacktrace(),
                        false,
                        false,
                        false,
                        true);
            }
        },

        /**
         * Normal Ruby-style backtrace, showing only Ruby and core class methods.
         */
        NORMAL {
            public BacktraceData getBacktraceData(ThreadContext context, Stream<StackWalker.StackFrame> stackStream) {
                return new BacktraceData(
                        stackStream,
                        context.getBacktrace(),
                        false,
                        false,
                        context.runtime.getInstanceConfig().getBacktraceMask(),
                        false);
            }
        },

        /**
         * Normal Ruby-style backtrace, showing only Ruby and core class methods.
         */
        CALLER {
            public BacktraceData getBacktraceData(ThreadContext context, Stream<StackWalker.StackFrame> stackStream) {
                return new BacktraceData(
                        stackStream,
                        context.getBacktrace(),
                        false,
                        false,
                        true,
                        false);
            }
        };

        /**
         * Gather current-stack backtrace data for a normal Ruby trace.
         *
         * @param context
         * @return
         */
        public BacktraceData getBacktraceData(ThreadContext context) {
            return WALKER.walk(Thread.currentThread().getStackTrace(), stream -> {
                BacktraceData data = getBacktraceData(context, stream);

                context.runtime.incrementBacktraceCount();
                if (RubyInstanceConfig.LOG_BACKTRACES) {
                    logBacktrace(context.runtime, data.getBacktrace(context.runtime));
                }

                return data;
            });
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
            Gather useGather = this == NORMAL ? INTEGRATED : this;

            return WALKER.walk(javaTrace, stream -> {
                BacktraceData data = useGather.getBacktraceData(context, stream);

                context.runtime.incrementBacktraceCount();
                if (RubyInstanceConfig.LOG_BACKTRACES)
                    logBacktrace(context.runtime, data.getBacktrace(context.runtime));

                return data;
            });
        }

        public abstract BacktraceData getBacktraceData(ThreadContext context, Stream<StackWalker.StackFrame> javaTrace);
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
                renderBacktraceJRuby(null, elts, buffer, color);
            }
        };

        public abstract String printBacktrace(RubyException exception, boolean console);
        public abstract void renderBacktrace(RubyStackTraceElement[] elts, StringBuilder buffer, boolean color);
    }

    public static String printFullMessage(ThreadContext context, IRubyObject exception, IRubyObject opts) {
        Ruby runtime = context.runtime;
        IRubyObject optArg = ArgsUtil.getOptionsArg(runtime, opts);
        boolean highlight = false;
        boolean reverse = false;

        if (!optArg.isNil()) {
            IRubyObject[] highlightOrder = ArgsUtil.extractKeywordArgs(context, (RubyHash) optArg, FULL_MESSAGE_KEYS);

            IRubyObject vHigh = highlightOrder[0];
            if (vHigh == null) vHigh = context.nil;
            if (vHigh != context.nil && vHigh != context.fals && vHigh != context.tru) {
                throw runtime.newArgumentError("expected true or false as highlight: " + vHigh);
            }
            highlight = vHigh.isTrue();

            IRubyObject vOrder = highlightOrder[1];
            if (vOrder != null) {
                vOrder = TypeConverter.checkID(vOrder);
                if (vOrder == runtime.newSymbol("bottom")) reverse = true;
                else if (vOrder == runtime.newSymbol("top")) reverse = false;
                else {
                    throw runtime.newArgumentError("expected :top or :bottom as order: " + vOrder);
                }
            }
        }

        // TODO: reverse
        return printBacktraceMRI(exception, highlight);
    }

    private static String printBacktraceMRI(IRubyObject exception, boolean console) {
        final Ruby runtime = exception.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();

        final IRubyObject backtrace = exception.callMethod(context, "backtrace");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream errorStream = new PrintStream(baos);
        boolean printedPosition = false;
        if (backtrace.isNil() || !(backtrace instanceof RubyArray)) {
            if (context.getFile() != null && context.getFile().length() > 0) {
                errorStream.print(context.getFile() + ':' + context.getLine());
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

        if (type == runtime.getRuntimeError() && (info == null || info.length() == 0)) {
            errorStream.print(": unhandled exception\n");
        } else {
            if (printedPosition) errorStream.print(": ");
            String path = type.getName();

            if (info.length() == 0) {
                errorStream.print(path + '\n');
            } else {
                if (path.startsWith("#")) {
                    path = null;
                }

                String tail = null;
                int idx = info.indexOf('\n');
                if (idx != -1) {
                    tail = info.substring(idx + 1);
                    info = info.substring(0, idx);
                }

                errorStream.print(info);

                if (path != null) {
                    errorStream.print(" (" + path + ")\n");
                } else {
                    errorStream.print('\n');
                }

                if (tail != null) {
                    errorStream.print(tail + '\n');
                }
            }
        }

        printBacktraceToStream(backtrace, errorStream, 1);

        return baos.toString();
    }

    private static final String FIRST_COLOR = "\033[0;31m";
    private static final String KERNEL_COLOR = "\033[0;36m";
    private static final String EVAL_COLOR = "\033[0;33m";
    private static final String CLEAR_COLOR = "\033[0m";

    public static String printBacktraceJRuby(Ruby runtime, RubyStackTraceElement[] frames, String type, String message, boolean color) {
        if (frames == null) frames = RubyStackTraceElement.EMPTY_ARRAY;

        StringBuilder buffer = new StringBuilder(64 + frames.length * 48);

        buffer.append(type).append(": ").append(message).append('\n');

        renderBacktraceJRuby(runtime, frames, buffer, color);

        return buffer.toString();
    }

    protected static String printBacktraceJRuby(RubyException exception, boolean console) {
        final Ruby runtime = exception.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();

        boolean color = console && runtime.getInstanceConfig().getBacktraceColor();

        // exception line
        String message;
        try {
            message = exception.callMethod(context, "message").toString();
        } catch (org.jruby.exceptions.Exception unused) {
            message = exception.message(context).toString();
        }
        if (exception.getMetaClass() == runtime.getRuntimeError() && message.length() == 0) {
            message = "No current exception";
        }
        String type = exception.getMetaClass().getName();

        return printBacktraceJRuby(exception.getRuntime(), exception.getBacktraceElements(), type, message, color);
    }

    private static void renderBacktraceJRuby(Ruby runtime, RubyStackTraceElement[] frames, StringBuilder buffer, boolean color) {
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
            String methodName = runtime == null ? frame.getMethodName() : runtime.newSymbol(frame.getMethodName()).idString();
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
        renderBacktraceMRI(trace, "", buffer, color);
    }

    private static void renderBacktraceMRI(RubyStackTraceElement[] trace, String linePrefix, StringBuilder buffer, boolean color) {
        for (int i = 0; i < trace.length; i++) {
            RubyStackTraceElement element = trace[i];
            buffer
                    .append(linePrefix)
                    .append(element.getFileName())
                    .append(':')
                    .append(element.getLineNumber())
                    .append(":in `")
                    .append(element.getMethodName())
                    .append("'\n");
        }
    }

    private static void printErrorPos(ThreadContext context, PrintStream errorStream) {
        if (context.getFile() != null && context.getFile().length() > 0) {
            if (context.getFrameName() != null) {
                errorStream.print(context.getFile() + ':' + context.getLine());
                errorStream.print(":in '" + context.getFrameName() + '\'');
            } else if (context.getLine() != 0) {
                errorStream.print(context.getFile() + ':' + context.getLine());
            } else {
                errorStream.print(context.getFile());
            }
        }
    }

    public static void printBacktraceToStream(IRubyObject backtrace, PrintStream errorStream, int skip) {
        if ( backtrace.isNil() ) return;
        if ( backtrace instanceof RubyArray ) {
            IRubyObject[] elements = ((RubyArray) backtrace).toJavaArrayMaybeUnsafe();
            for (int i = skip; i < elements.length; i++) {
                IRubyObject stackTraceLine = elements[i];
                if (stackTraceLine instanceof RubyString) {
                    errorStream.println("\tfrom " + stackTraceLine);
                }
                else {
                    errorStream.println("\t" + stackTraceLine);
                }
            }
        }
    }

    public static IRubyObject generateMRIBacktrace(Ruby runtime, RubyStackTraceElement[] trace) {
        if (trace == null) return runtime.getNil();

        ThreadContext context = runtime.getCurrentContext();
        final IRubyObject[] traceArray = new IRubyObject[trace.length];

        for (int i = 0; i < trace.length; i++) {
            traceArray[i] = RubyStackTraceElement.to_s_mri(context, trace[i]);
        }

        return RubyArray.newArrayMayCopy(runtime, traceArray);
    }

    @Deprecated
    public RubyStackTraceElement getBacktraceElement(ThreadContext context, int uplevel) {
        // NOTE: could be optimized not to walk the whole stack
        RubyStackTraceElement[] elements = getBacktrace(context).getBacktrace(context.runtime);

        // User can ask for level higher than stack
        if (elements.length <= uplevel + 1) uplevel = -1;

        return elements[uplevel + 1];
    }
}
