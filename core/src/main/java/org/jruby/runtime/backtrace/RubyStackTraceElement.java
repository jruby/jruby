package org.jruby.runtime.backtrace;

import org.jruby.runtime.backtrace.FrameType;

public class RubyStackTraceElement {
    public static final RubyStackTraceElement[] EMPTY_ARRAY = new RubyStackTraceElement[0];
    private final StackTraceElement element;
    private final boolean binding;
    private final FrameType frameType;

    public RubyStackTraceElement(StackTraceElement element) {
        this.element = element;
        this.binding = false;
        this.frameType = FrameType.METHOD;
    }

    public RubyStackTraceElement(String cls, String method, String file, int line, boolean binding) {
        this(cls, method, file, line, binding, FrameType.METHOD);
    }

    public RubyStackTraceElement(String cls, String method, String file, int line, boolean binding, FrameType frameType) {
        this.element = new StackTraceElement(cls, method, file, line);
        this.binding = binding;
        this.frameType = frameType;
    }

    public StackTraceElement getElement() {
        return element;
    }

    public boolean isBinding() {
        return binding;
    }

    public String getClassName() {
        return element.getClassName();
    }

    public String getFileName() {
        return element.getFileName();
    }

    public int getLineNumber() {
        return element.getLineNumber();
    }

    public String getMethodName() {
        return element.getMethodName();
    }

    public FrameType getFrameType() {
        return frameType;
    }

    public String toString() {
        return element.toString();
    }
    
    public String mriStyleString() {
        return element.getFileName() + ":" + element.getLineNumber() + ":in `" + element.getMethodName() + "'";
    }
}
