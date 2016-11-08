package org.jruby.runtime.backtrace;

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

    public final String getMethodName() {
        return methodName;
    }

    public final FrameType getFrameType() {
        return frameType;
    }

    private transient StackTraceElement element;

    public final StackTraceElement asStackTraceElement() {
        if ( element != null ) return element;
        return element = new StackTraceElement(className, methodName, fileName, lineNumber);
    }

    @Deprecated
    public StackTraceElement getElement() { return asStackTraceElement(); }

    public String toString() {
        return asStackTraceElement().toString();
    }

    public final CharSequence mriStyleString() {
        // return fileName + ':' + lineNumber + ":in `" + methodName + '\'';
        return new StringBuilder(fileName.length() + methodName.length() + 12).
                append(fileName).append(':').append(lineNumber).
                append(":in `").append(methodName).append('\'');
    }

}
