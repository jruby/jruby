package org.jruby.runtime.backtrace;

import org.jruby.lexer.yacc.ISourcePosition;

public class BacktraceElement {

    public BacktraceElement() {
    }

    public BacktraceElement(String method, String filename, int line) {
        this.method = method;
        this.filename = filename;
        this.line = line;
    }

    @Override
    public String toString() {
        return method + " at " + filename + ":" + line;
    }

    @Override
    public BacktraceElement clone() {
        return new BacktraceElement(method, filename, line);
    }

    public static void update(BacktraceElement backtrace, String method, ISourcePosition position) {
        backtrace.method = method;
        backtrace.filename = position.getFile();
        backtrace.line = position.getLine();
    }

    public static void update(BacktraceElement backtrace, String method, String file, int line) {
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
    public String method;
    public String filename;
    public int line;
}
