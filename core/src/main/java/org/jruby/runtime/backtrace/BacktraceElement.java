package org.jruby.runtime.backtrace;

import org.jruby.RubyModule;

public final class BacktraceElement implements Cloneable {

    public static final BacktraceElement[] EMPTY_ARRAY = new BacktraceElement[0];

    public BacktraceElement() {
    }

    public BacktraceElement(String method, String filename, int line) {
        this.method = method;
        this.filename = filename;
        this.line = line;
    }

    @Override
    public String toString() {
        return (klass == null ? "" : klass + "#")
                + method
                + " at "
                + filename
                + ':'
                + line;
    }

    @Override
    public BacktraceElement clone() {
        return new BacktraceElement(method, filename, line);
    }

    public static void update(BacktraceElement backtrace, String method, String file, int line) {
        backtrace.method = method;
        backtrace.filename = file;
        backtrace.line = line;
    }

    public static void update(BacktraceElement backtrace, String klass, String method, String file, int line) {
        backtrace.klass = klass;
        backtrace.method = method;
        backtrace.filename = file;
        backtrace.line = line;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getModule() {
        return klass;
    }

    public void setModule(String klass) {
        this.klass = klass;
    }

    public String klass;
    public String method;
    public String filename;
    public int line;
}
