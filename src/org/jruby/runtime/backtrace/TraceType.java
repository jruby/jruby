package org.jruby.runtime.backtrace;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.PrintStream;
import java.util.ArrayList;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyException;
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

    public BacktraceData getBacktrace(ThreadContext context, boolean nativeException) {
        return gather.getBacktraceData(context, nativeException);
    }

    public String printBacktrace(RubyException exception, boolean console) {
        return format.printBacktrace(exception, console);
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

    public static TraceType traceTypeFor(String style) {
        if (style.equalsIgnoreCase("raw")) return new TraceType(Gather.RAW, Format.JRUBY);
        else if (style.equalsIgnoreCase("ruby_framed")) return new TraceType(Gather.NORMAL, Format.JRUBY);
        else if (style.equalsIgnoreCase("normal")) return new TraceType(Gather.NORMAL, Format.JRUBY);
        else if (style.equalsIgnoreCase("rubinius")) return new TraceType(Gather.NORMAL, Format.RUBINIUS);
        else if (style.equalsIgnoreCase("full")) return new TraceType(Gather.FULL, Format.JRUBY);
        else if (style.equalsIgnoreCase("mri")) return new TraceType(Gather.NORMAL, Format.MRI);
        else return new TraceType(Gather.NORMAL, Format.JRUBY);
    }
    
    public enum Gather {
        /**
         * Full raw backtraces with all Java frames included.
         */
        RAW {
            public BacktraceData getBacktraceData(ThreadContext context, Thread thread, boolean nativeException) {
                return new BacktraceData(
                        thread.getStackTrace(),
                        new BacktraceElement[0],
                        true,
                        false,
                        this);
            }
        },

        /**
         * A backtrace with interpreted frames intact, but don't remove Java frames.
         */
        FULL {
            public BacktraceData getBacktraceData(ThreadContext context, Thread thread, boolean nativeException) {
        return new BacktraceData(
                        thread.getStackTrace(),
                        context.createBacktrace2(0, nativeException),
                        true,
                        false,
                        this);
            }
        },

        /**
         * Normal Ruby-style backtrace, showing only Ruby and core class methods.
         */
        NORMAL {
            public BacktraceData getBacktraceData(ThreadContext context, Thread thread, boolean nativeException) {
                return new BacktraceData(
                        thread.getStackTrace(),
                        context.createBacktrace2(0, nativeException),
                        false,
                        false,
                        this);
            }
        },

        /**
         * Normal Ruby-style backtrace, showing only Ruby and core class methods.
         */
        CALLER {
            public BacktraceData getBacktraceData(ThreadContext context, Thread thread, boolean nativeException) {
                return new BacktraceData(
                        thread.getStackTrace(),
                        context.createBacktrace2(0, nativeException),
                        false,
                        true,
                        this);
            }
        };

        public BacktraceData getBacktraceData(ThreadContext context, boolean nativeException) {
            return getBacktraceData(context, Thread.currentThread(), nativeException);
        }
        public abstract BacktraceData getBacktraceData(ThreadContext context, Thread thread, boolean nativeException);
    }
    
    public enum Format {
        /**
         * Formatting like C Ruby
         */
        MRI {
            public String printBacktrace(RubyException exception, boolean console) {
                return printBacktraceMRI(exception, console);
            }
        },

        /**
         * New JRuby formatting
         */
        JRUBY {
            public String printBacktrace(RubyException exception, boolean console) {
                return printBacktraceJRuby(exception, console);
            }
        },

        /**
         * Rubinius-style formatting
         */
        RUBINIUS {
            public String printBacktrace(RubyException exception, boolean console) {
                return printBacktraceRubinius(exception, console);
            }
        };

        public abstract String printBacktrace(RubyException exception, boolean console);
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

        exception.printBacktrace(errorStream);

        return new String(baos.toByteArray());
    }

    private static final String FIRST_COLOR = "\033[0;31m";
    private static final String KERNEL_COLOR = "\033[0;36m";
    private static final String EVAL_COLOR = "\033[0;33m";
    private static final String CLEAR_COLOR = "\033[0m";

    protected static String printBacktraceRubinius(RubyException exception, boolean console) {
        Ruby runtime = exception.getRuntime();
        RubyStackTraceElement[] frames = exception.getBacktraceElements();
        if (frames == null) frames = new RubyStackTraceElement[0];

        ArrayList firstParts = new ArrayList();
        int longestFirstPart = 0;
        for (RubyStackTraceElement frame : frames) {
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
        for (RubyStackTraceElement frame : frames) {
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

    protected static String printBacktraceJRuby(RubyException exception, boolean console) {
        Ruby runtime = exception.getRuntime();
        RubyStackTraceElement[] frames = exception.getBacktraceElements();
        if (frames == null) frames = new RubyStackTraceElement[0];

        // find longest method name
        int longestMethod = 0;
        for (RubyStackTraceElement frame : frames) {
            longestMethod = Math.max(longestMethod, frame.getMethodName().length());
        }

        StringBuilder buffer = new StringBuilder();

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
        
        boolean color = console && runtime.getInstanceConfig().getBacktraceColor();

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

    protected static BacktraceData getBacktrace(ThreadContext context, Gather gather, boolean nativeException, boolean full, boolean maskNative) {
        return new BacktraceData(
                Thread.currentThread().getStackTrace(),
                context.createBacktrace2(0, nativeException),
                full,
                maskNative,
                gather);
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