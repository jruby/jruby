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
import org.jruby.RubySymbol;
import org.jruby.api.Convert;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.jruby.util.TypeConverter;

import static org.jruby.api.Access.exceptionClass;
import static org.jruby.api.Access.instanceConfig;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Create.newEmptyString;
import static org.jruby.api.Create.newHash;
import static org.jruby.api.Error.argumentError;
import static org.jruby.util.RubyStringBuilder.str;

public class TraceType {

    private static final Logger LOG = LoggerFactory.getLogger(TraceType.class);
    private static final StackWalker WALKER = ThreadContext.WALKER;

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
    @Deprecated(since = "9.4-", forRemoval = true)
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
    @Deprecated(since = "9.4-", forRemoval = true)
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
    @Deprecated(since = "9.4-", forRemoval = true)
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
    @Deprecated(since = "9.4-", forRemoval = true)
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
                        instanceConfig(context).getBacktraceMask(),
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
                RubyHash opts = RubyHash.newKwargs(exception.getRuntime(), "highlight", exception.getRuntime().newBoolean(console));
                return printBacktraceMRI(exception, opts, console, false).toString();
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

    public static IRubyObject printDetailedMessage(ThreadContext context, IRubyObject exception, IRubyObject opts) {
        IRubyObject optArg = ArgsUtil.getOptionsArg(context, opts);
        IRubyObject highlightArg = checkHighlightKeyword(context, optArg, false);
        RubyString errorStream = newEmptyString(context);

        printErrMessageToStream(exception, errorStream, highlightArg.isTrue());

        return errorStream;
    }

    public static RubyString printFullMessage(ThreadContext context, IRubyObject exception, IRubyObject opts) {
        IRubyObject optArg = ArgsUtil.getOptionsArg(context, opts);
        IRubyObject highlightArg = checkHighlightKeyword(context, optArg, true);
        boolean reverse = checkOrderKeyword(context, optArg);

        if (optArg.isNil()) optArg = newHash(context);

        ((RubyHash) optArg).fastASet(asSymbol(context, "highlight"), highlightArg);

        return printBacktraceMRI(exception, optArg, highlightArg.isTrue(), reverse);
    }

    private static boolean checkOrderKeyword(ThreadContext context, IRubyObject optArg) {
        boolean reverse;
        if (optArg.isNil()) {
            reverse = false;
        } else {
            RubyHash optHash = (RubyHash) optArg;
            IRubyObject highlightOrder = optHash.fastARef(asSymbol(context, "order"));
            reverse = determineDirection(context, highlightOrder);
        }
        return reverse;
    }

    private static IRubyObject checkHighlightKeyword(ThreadContext context, IRubyObject optArg, boolean autoTTYDetect) {
        IRubyObject highlightArg = context.nil;

        RubySymbol highlightSym = Convert.asSymbol(context, "highlight");
        if (!optArg.isNil()) {
            RubyHash optHash = (RubyHash) optArg;

            highlightArg = optHash.fastARef(highlightSym);

            if (highlightArg == null) highlightArg = context.nil;
            if (!(highlightArg.isNil() || highlightArg == context.tru || highlightArg == context.fals)) {
                throw argumentError(context, "expected true or false as highlight: " + highlightArg);
            }
        }

        if (highlightArg.isNil()) {
            highlightArg = asBoolean(context, autoTTYDetect && RubyException.to_tty_p(context, exceptionClass(context)).isTrue());
        }

        return highlightArg;
    }

    private static boolean determineDirection(ThreadContext context, IRubyObject vOrder) {
        if (vOrder == null || vOrder.isNil()) return false;

        IRubyObject id = TypeConverter.checkID(vOrder);
        if (id == Convert.asSymbol(context, "bottom")) return true;
        if (id == Convert.asSymbol(context, "top")) return false;
        throw argumentError(context, str(context.runtime, "expected :top or :bottom as order: ", vOrder));
    }

    private static RubyString printBacktraceMRI(IRubyObject exception, IRubyObject opts, boolean highlight, boolean reverse) {
        RubyString errorStream = newEmptyString(exception.getRuntime().getCurrentContext());

        if (reverse) {
            if (highlight) errorStream.catString(BOLD);
            errorStream.catString("Traceback");
            if (highlight) errorStream.catString(RESET);
            errorStream.catString(" (most recent call last):\n");
        }

        final Set<Object> shownCauses = new HashSet<>();
        printExceptionToStream(exception, errorStream, opts, highlight, reverse, shownCauses);

        return errorStream;
    }

    private static void printExceptionToStream(IRubyObject exception, RubyString errorStream, IRubyObject opts,
                                               boolean highlight, boolean reverse, Set<Object> shownCauses) {
        final Ruby runtime = exception.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        final IRubyObject backtrace = exception.callMethod(context, "backtrace");

        if (reverse) {
            printCauseToStream(context, exception, errorStream, opts, highlight, reverse, shownCauses);
            printBacktraceToStream(context, backtrace, errorStream, reverse, 1);
            printErrInfoToStream(exception, backtrace, errorStream, opts, highlight);
        } else {
            printErrInfoToStream(exception, backtrace, errorStream, opts, highlight);
            printBacktraceToStream(context, backtrace, errorStream, reverse, 1);
            printCauseToStream(context, exception, errorStream, opts, highlight, reverse, shownCauses);
        }
    }

    private static void printCauseToStream(ThreadContext context, IRubyObject exception, RubyString errorStream,
                                           IRubyObject opts, boolean highlight, boolean reverse, Set<Object> shownCauses) {
        IRubyObject cause = exception.callMethod(context, "cause");
        if (!cause.isNil() && !shownCauses.contains(cause)) {
            shownCauses.add(cause);
            printExceptionToStream(cause, errorStream, opts, highlight, reverse, shownCauses);
        }
    }

    private static final String UNDERLINE = "\033[1;4m";
    private static final String BOLD = "\033[1m";
    private static final String RESET = "\033[m";

    private static void printErrInfoToStream(IRubyObject exception, IRubyObject backtrace, RubyString errorStream,
                                             IRubyObject opts, boolean highlight) {
        final Ruby runtime = exception.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();

        boolean printedPosition = false;
        if (backtrace == null || backtrace.isNil() || !(backtrace instanceof RubyArray)) {
            if (context.getFile() != null && context.getFile().length() > 0) {
                errorStream.catString(context.getFile() + ':' + (context.getLine() + 1));
                printedPosition = true;
            } else {
                errorStream.catString(""+(context.getLine() + 1));
                printedPosition = true;
            }
            // When in no backtrace exception (like just making it) it should print the caller.
            // As far as I can see currently this is only full_message but I try to retrieve it
            // from the frame.
            String method = context.getFrameName();
            if (method == null) method = "full_message";
            errorStream.catString(":in '");
            errorStream.catString(method);
            errorStream.cat('\'');
        } else if (((RubyArray) backtrace).getLength() == 0) {
            printErrorPos(context, errorStream);
        } else {
            IRubyObject mesg = ((RubyArray) backtrace).first(context);

            if (mesg.isNil()) {
                printErrorPos(context, errorStream);
            } else {
                errorStream.append(mesg);
                printedPosition = true;
            }
        }

        RubyClass type = exception.getMetaClass();
        String info = exception.toString();

        if (type == runtime.getRuntimeError() && (info == null || info.length() == 0)) {
            errorStream.catString(": ");
        } else {
            if (printedPosition) errorStream.catString(": ");
        }

        IRubyObject message = context.nil;
        if (exception.respondsTo("detailed_message")) {
            if (opts.isNil()) {
                message = exception.callMethod(context, "detailed_message");
            } else {
                context.callInfo = ThreadContext.CALL_KEYWORD | ThreadContext.CALL_KEYWORD_REST;
                message = exception.callMethod(context, "detailed_message", opts);
            }
        }
        if (message instanceof RubyString str) {
            errorStream.append(str);
        } else if (message.isNil()) {
            if (highlight) errorStream.catString(UNDERLINE);
            errorStream.append(type.getRealClass().rubyName(context));
            if (highlight) errorStream.catString(RESET);

        } else {
            errorStream.append(message.convertToString());
        }

        errorStream.cat('\n');
    }

    private static void printErrMessageToStream(IRubyObject exception, RubyString errorStream, boolean highlight) {
        RubyClass type = exception.getMetaClass();
        String info = exception.toString();

        if (type == exception.getRuntime().getRuntimeError() && (info == null || info.length() == 0)) {

            if (highlight) errorStream.catString(UNDERLINE);
            errorStream.catString("unhandled exception");
            if (highlight) errorStream.catString(RESET);
        } else {
            String path = type.getName();

            if (info.length() == 0) {
                if (highlight) errorStream.catString(UNDERLINE);
                errorStream.catString(path);
                if (highlight) errorStream.catString(RESET);
            } else {
                if (highlight) errorStream.catString(BOLD);

                if (path.startsWith("#")) {
                    path = null;
                }

                String tail = null;
                int idx = info.indexOf('\n');
                if (idx != -1) {
                    tail = info.substring(idx + 1);
                    info = info.substring(0, idx);
                }

                errorStream.catString(info);

                if (path != null) {
                    errorStream.catString(" (");
                    if (highlight) errorStream.catString(UNDERLINE);
                    errorStream.catString(path);
                    if (highlight) {
                        errorStream.catString(RESET);
                        errorStream.catString(BOLD);
                    }
                    errorStream.cat(')');
                    if (highlight) errorStream.catString(RESET);
                }

                if (tail != null && tail.length() > 0) {
                    errorStream.cat('\n');
                    if (!highlight) {
                        errorStream.catString(tail);
                    } else {
                        int start = 0, end = tail.indexOf('\n');
                        while (end != -1) {
                            errorStream.catString(BOLD);
                            errorStream.catString(tail.substring(start, end));
                            errorStream.catString(RESET);
                            errorStream.cat('\n');
                            start = end + 1;
                            end = tail.indexOf('\n', start);
                        }
                        errorStream.catString(BOLD);
                        errorStream.catString(tail.substring(start));
                        errorStream.catString(RESET);
                    }
                }
            }
        }
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
                } else if (frame.isBinding() || frame.getFileName().startsWith("(eval")) {
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
                    .append(":in '")
                    .append(element.getMethodName())
                    .append("'\n");
        }
    }

    private static void printErrorPos(ThreadContext context, RubyString errorStream) {
        if (context.getFile() != null && context.getFile().length() > 0) {
            if (context.getFrameName() != null) {
                errorStream.catString(context.getFile() + ':' + context.getLine());
                errorStream.catString(":in '" + context.getFrameName() + '\'');
            } else if (context.getLine() != 0) {
                errorStream.catString(context.getFile() + ':' + context.getLine());
            } else {
                errorStream.catString(context.getFile());
            }
        }
    }

    public static void printBacktraceToStream(ThreadContext context, IRubyObject backtrace, RubyString errorStream, int skip) {
        printBacktraceToStream(context, backtrace, errorStream, false, skip);
    }

    public static void printBacktraceToStream(ThreadContext context, IRubyObject backtrace, RubyString errorStream, boolean reverse, int skip) {
        if (backtrace.isNil()) return;
        if (backtrace instanceof RubyArray bt) {
            IRubyObject[] elements = bt.toJavaArrayMaybeUnsafe();
            int optionBacktraceLimit = instanceConfig(context).getBacktraceLimit();
            int limitPlusSkip = optionBacktraceLimit + skip;
            int maxBacktraceLines = optionBacktraceLimit == -1 || limitPlusSkip > elements.length ? elements.length : limitPlusSkip;

            int i, len = maxBacktraceLines;
            final int threshold = 1000000000;
            final int width = (len <= 1) ? Integer.MIN_VALUE : ((int) Math.log10((len > threshold ?
                    ((len - 1) / threshold) :
                    len - 1)) +
                    (len < threshold ? 0 : 9) + 1);

            for (i = skip; i < maxBacktraceLines; i++) {
                IRubyObject stackTraceLine = elements[reverse ? len - i : i];
                errorStream.cat('\t');
                if (reverse) {
                    errorStream.catString(String.format("%" + width + "d: ", len - i));
                }
                if (stackTraceLine instanceof RubyString) {
                    errorStream.catString("from " + stackTraceLine);
                    errorStream.cat('\n');
                }
                else {
                    errorStream.append(stackTraceLine);
                }
            }

            if ((elements.length > i)) {
                String suppressedLines = String.valueOf(elements.length - (i));
                errorStream.catString("\t ... " + suppressedLines + " levels...\n");
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
        return filename != null && filename.startsWith("uri:classloader:/jruby/kernel/");
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
