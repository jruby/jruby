package org.jruby.runtime.backtrace;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
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
     * @deprecated use {@link #logWarning(org.jruby.runtime.backtrace.RubyStackTraceElement[])}
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
                        true,
                        false);
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
                        false,
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
                        false,
                        false);
            }
        },

        /**
         * Warning traces, showing only Ruby and core class methods, excluding internal files.
         */
        WARN {
            public BacktraceData getBacktraceData(ThreadContext context, Stream<StackWalker.StackFrame> stackStream) {
                return new BacktraceData(
                        stackStream,
                        context.getBacktrace(),
                        false,
                        false,
                        true,
                        false,
                        true);
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
                return printBacktraceMRI(exception, console, false);
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
        IRubyObject vHigh= context.nil;
        IRubyObject vOrder = context.nil;

        if (!optArg.isNil()) {
            IRubyObject[] highlightOrder = ArgsUtil.extractKeywordArgs(context, (RubyHash) optArg, FULL_MESSAGE_KEYS);

            if (highlightOrder[0] != null) {
                vHigh = highlightOrder[0];
                if (vHigh != context.nil && vHigh != context.fals && vHigh != context.tru) {
                    throw runtime.newArgumentError("expected true or false as highlight: " + vHigh);
                }
            }

            if (highlightOrder[1] != null) {
                vOrder = highlightOrder[1];
                if (!vOrder.isNil()) {
                    IRubyObject id = TypeConverter.checkID(vOrder);
                    if (id == runtime.newSymbol("bottom")) vOrder = context.tru;
                    else if (id == runtime.newSymbol("top")) vOrder = context.fals;
                    else {
                        throw runtime.newArgumentError("expected :top or :bottom as order: " + vOrder);
                    }
                }
            }
        }

        if (vHigh.isNil()) {
            vHigh = RubyException.to_tty_p(context, context.runtime.getException());
        }
        if (vOrder.isNil()) {
            vOrder = context.fals;
        }

        boolean highlight = vHigh.isTrue();
        boolean reverse = vOrder.isTrue();

        return printBacktraceMRI(exception, highlight, reverse);
    }

    private static String printBacktraceMRI(IRubyObject exception, boolean highlight, boolean reverse) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream errorStream = new PrintStream(baos);

        if (reverse) {
            if (highlight) errorStream.print(BOLD);
            errorStream.print("Traceback");
            if (highlight) errorStream.print(RESET);
            errorStream.print(" (most recent call last):\n");
        }

        final Set<Object> shownCauses = new HashSet<>();
        printExceptionToStream(exception, errorStream, highlight, reverse, shownCauses);

        return baos.toString();
    }

    private static void printExceptionToStream(IRubyObject exception, PrintStream errorStream, boolean highlight, boolean reverse, Set<Object> shownCauses) {
        final Ruby runtime = exception.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        final IRubyObject backtrace = exception.callMethod(context, "backtrace");

        if (reverse) {
            printCauseToStream(exception, errorStream, highlight, reverse, shownCauses);
            printBacktraceToStream(backtrace, errorStream, reverse, 1);
            printErrInfoToStream(exception, backtrace, errorStream, highlight);
        } else {
            printErrInfoToStream(exception, backtrace, errorStream, highlight);
            printBacktraceToStream(backtrace, errorStream, reverse, 1);
            printCauseToStream(exception, errorStream, highlight, reverse, shownCauses);
        }
    }

    private static void printCauseToStream(IRubyObject exception, PrintStream errorStream, boolean highlight, boolean reverse, Set<Object> shownCauses) {
        final Ruby runtime = exception.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();

        IRubyObject cause = exception.callMethod(context, "cause");
        if (!cause.isNil() && !shownCauses.contains(cause)) {
            shownCauses.add(cause);
            printExceptionToStream(cause, errorStream, highlight, reverse, shownCauses);
        }
    }

    private static final String UNDERLINE = "\033[1;4m";
    private static final String BOLD = "\033[1m";
    private static final String RESET = "\033[m";

    private static void printErrInfoToStream(IRubyObject exception, IRubyObject backtrace, PrintStream errorStream, boolean highlight) {
        final Ruby runtime = exception.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();

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
            errorStream.print(": ");
            if (highlight) errorStream.print(UNDERLINE);
            errorStream.print("unhandled exception");
            if (highlight) errorStream.print(RESET);
        } else {
            if (printedPosition) errorStream.print(": ");
            String path = type.getName();

            if (info.length() == 0) {
                if (highlight) errorStream.print(UNDERLINE);
                errorStream.print(path);
                if (highlight) errorStream.print(RESET);
            } else {
                if (highlight) errorStream.print(BOLD);

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
                    errorStream.print(" (");
                    if (highlight) errorStream.print(UNDERLINE);
                    errorStream.print(path);
                    if (highlight) {
                        errorStream.print(RESET);
                        errorStream.print(BOLD);
                    }
                    errorStream.print(')');
                    if (highlight) errorStream.print(RESET);
                }

                if (tail != null) {
                    errorStream.print('\n');
                    if (!highlight) {
                        errorStream.print(tail);
                    } else {
                        int start = 0, end = tail.indexOf('\n');
                        while (end != -1) {
                            errorStream.print(BOLD);
                            errorStream.print(tail.substring(start, end));
                            errorStream.print(RESET);
                            errorStream.print('\n');
                            start = end + 1;
                            end = tail.indexOf('\n', start);
                        }
                        errorStream.print(BOLD);
                        errorStream.print(tail.substring(start));
                        errorStream.print(RESET);
                    }
                }
            }
        }
        errorStream.print('\n');
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
        console = console && runtime.getInstanceConfig().getBacktraceColor();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream errorStream = new PrintStream(baos);

        printBacktraceJRubyToStream(exception, errorStream, console);
        Set<Object> shownCauses = new HashSet<>();

        for (
                Object cause = exception.getCause();
                cause instanceof RubyException && !shownCauses.contains(cause);
                cause = ((RubyException) cause).getCause()) {

            printBacktraceJRubyToStream((RubyException) cause, errorStream, console);
            shownCauses.add(cause);
        }

        return baos.toString();
    }

    private static void printBacktraceJRubyToStream(RubyException exception, PrintStream errorStream, boolean console) {
        final Ruby runtime = exception.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();

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

        errorStream.print(printBacktraceJRuby(runtime, exception.getBacktraceElements(), type, message, console));
    }

    private static void renderBacktraceJRuby(Ruby runtime, RubyStackTraceElement[] frames, StringBuilder buffer, boolean color) {
        int optionBacktraceLimit = Integer.MAX_VALUE;
        int maxBacktraceLines =  frames.length;

        // find longest method name
        int longestMethod = 0;
        for (RubyStackTraceElement frame : frames) {
            longestMethod = Math.max(longestMethod, frame.getMethodName().length());
        }

        if (runtime != null) {
            optionBacktraceLimit = runtime.getInstanceConfig().getBacktraceLimit();
            maxBacktraceLines =  (optionBacktraceLimit > frames.length || optionBacktraceLimit == -1) ? frames.length : optionBacktraceLimit;
        }

        // backtrace lines
        boolean first = true;
        for (int i=0; i < maxBacktraceLines; i++) {
            RubyStackTraceElement frame = frames[i];

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

            buffer.append('\n');
        }

        if (runtime != null && (frames.length > maxBacktraceLines)) {
            String suppressedLines = String.valueOf(frames.length - maxBacktraceLines);
            buffer.append("... " + suppressedLines + " levels...\n");
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
        printBacktraceToStream(backtrace, errorStream, false, skip);
    }

    public static void printBacktraceToStream(IRubyObject backtrace, PrintStream errorStream, boolean reverse, int skip) {
        if ( backtrace.isNil() ) return;
        if ( backtrace instanceof RubyArray ) {
            IRubyObject[] elements = ((RubyArray) backtrace).toJavaArrayMaybeUnsafe();
            Ruby runtime = backtrace.getRuntime();
            int optionBacktraceLimit = runtime.getInstanceConfig().getBacktraceLimit();
            int limitPlusSkip = optionBacktraceLimit + skip;
            int maxBacktraceLines =  (optionBacktraceLimit == -1 || limitPlusSkip > elements.length) ? elements.length : limitPlusSkip;

            int i, len = maxBacktraceLines;
            final int threshold = 1000000000;
            final int width = (len <= 1) ? Integer.MIN_VALUE : ((int) Math.log10((double) (len > threshold ?
                    ((len - 1) / threshold) :
                    len - 1)) +
                    (len < threshold ? 0 : 9) + 1);

            for (i = skip; i < maxBacktraceLines; i++) {
                IRubyObject stackTraceLine = elements[reverse ? len - i : i];
                errorStream.print('\t');
                if (reverse) {
                    errorStream.printf("%" + width + "d: ", len - i);
                }
                if (stackTraceLine instanceof RubyString) {
                    errorStream.println("from " + stackTraceLine);
                }
                else {
                    errorStream.println(stackTraceLine);
                }
            }

            if ((elements.length > i)) {
                String suppressedLines = String.valueOf(elements.length - (i));
                errorStream.append("\t ... " + suppressedLines + " levels...\n");
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

    public static String maskInternalFiles(String filename) {
        if (isInternalFile(filename)) {
            return "<internal:" + filename + ">";
        }

        return filename;
    }

    public static boolean isInternalFile(String filename) {
        return filename.startsWith("uri:classloader:/jruby/kernel/");
    }

    public static boolean hasInternalMarker(String filename) {
        return filename.startsWith("<internal:");
    }

    public static boolean isExcludedInternal(String filename) {
        return TraceType.isInternalFile(filename) || TraceType.hasInternalMarker(filename);
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
