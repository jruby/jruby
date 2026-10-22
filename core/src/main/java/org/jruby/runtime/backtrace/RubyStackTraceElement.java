package org.jruby.runtime.backtrace;

import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;
import org.jruby.util.ConvertBytes;

import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Create.newString;

public class RubyStackTraceElement implements java.io.Serializable {
    public static final RubyStackTraceElement[] EMPTY_ARRAY = new RubyStackTraceElement[0];

    private final String className;
    private final String methodName;
    private final String fileName;
    private final int    lineNumber;
    private final boolean binding;
    private final FrameType frameType;

    public RubyStackTraceElement(StackTraceElement element) {
        this.className = element.getClassName();
        this.methodName = element.getMethodName();
        this.fileName = (element.getFileName() == null) ? "unknown" : element.getFileName();
        this.lineNumber = element.getLineNumber();
        this.binding = false;
        this.frameType = FrameType.METHOD;

        this.element = element;
    }

    public RubyStackTraceElement(String klass, String method, String file, int line, boolean binding) {
        this(klass, method, file, line, binding, FrameType.METHOD);
    }

    public RubyStackTraceElement(String klass, String method, String file, int line, boolean binding, FrameType frameType) {
        this.className = klass;
        this.methodName = method;
        this.fileName = (file == null) ? "unknown" : file;
        this.lineNumber = line;
        this.binding = binding;
        this.frameType = frameType;
    }

    public final boolean isBinding() {
        return binding;
    }

    public final String getClassName() {
        return className;
    }

    public final String getFileName() {
        return fileName;
    }

    public final int getLineNumber() {
        return lineNumber;
    }

    public String getFileAndLine() {
        return "" + fileName + ":" + lineNumber;
    }

    public final String getMethodName() {
        return methodName;
    }

    /**
     * The method name without its Ruby 3.4+ owner qualifier: "each" for
     * "Array#each", "sqrt" for "Integer.sqrt", "x" for "A::B#x".
     */
    /** "block in " for a block frame, or "block " when its name already carries "(N levels) in ". */
    public static String blockPrefix(String name) {
        if (name == null) return "block in ";
        if (name.startsWith("block ")) return "";  // interpreted frames carry the full label
        return name.startsWith("(") && name.contains(" levels) in ") ? "block " : "block in ";
    }

    public static String baseName(String label) {
        if (label == null) return null;
        if (label.startsWith("block in ")) label = label.substring("block in ".length());
        else if (label.startsWith("block (")) label = label.substring("block ".length());
        int levels = label.indexOf(" levels) in ");
        if (label.startsWith("(") && levels > 0) label = label.substring(levels + " levels) in ".length());
        if (label.startsWith("<")) return label; // <main>, <class:C>, <top (required)>
        int hash = label.lastIndexOf('#');
        if (hash > 0) return label.substring(hash + 1);
        int dot = label.lastIndexOf('.');
        if (dot > 0 && dot < label.length() - 1 && Character.isUpperCase(label.charAt(0))) return label.substring(dot + 1);
        return label;
    }

    public final FrameType getFrameType() {
        return frameType;
    }

    private transient StackTraceElement element;

    public final StackTraceElement asStackTraceElement() {
        if ( element != null ) return element;
        // the Java view keeps the plain method name ("times", not "Integer#times")
        return element = new StackTraceElement(className, baseName(methodName), fileName, lineNumber);
    }

    public String toString() {
        return asStackTraceElement().toString();
    }

    public static RubyString to_s_mri(ThreadContext context, RubyStackTraceElement element) {
        RubySymbol methodSym = asSymbol(context, element.getMethodName());
        RubyString line = newString(context, new ByteList(methodSym.getBytes().length() + element.getFileName().length() + 18));

        line.setEncoding(methodSym.getEncoding());

        line.catString(element.getFileName());
        line.cat(CommonByteLists.COLON);
        line.cat(ConvertBytes.longToByteListCached(element.getLineNumber()));
        line.cat(CommonByteLists.BACKTRACE_IN);
        if (element.getFrameType() == FrameType.BLOCK) line.catString(blockPrefix(element.getMethodName()));
        line.cat(methodSym.getBytes());
        line.cat('\'');

        return line;
    }

}
