package org.jruby.runtime.backtrace;

import org.jruby.lexer.yacc.ISourcePosition;

public class BacktraceElement {

    public BacktraceElement() {
    }

    public BacktraceElement(String klass, String method, String filename, int line) {
        this.method = method;
        this.filename = filename;
        this.line = line;
        this.klass = klass;
    }

    @Override
    public String toString() {
        return klass + "#" + method + " at " + filename + ":" + line;
    }

    @Override
    public BacktraceElement clone() {
        return new BacktraceElement(klass, method, filename, line);
    }

    public static void update(BacktraceElement backtrace, String klass, String method, ISourcePosition position) {
        backtrace.method = method;
        backtrace.filename = position.getFile();
        backtrace.line = position.getLine();
        backtrace.klass = klass;
    }

    public static void update(BacktraceElement backtrace, String klass, String method, String file, int line) {
        backtrace.method = method;
        backtrace.filename = file;
        backtrace.line = line;
        backtrace.klass = klass;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getKlass() {
        return klass;
    }

    public void setKlass(String klass) {
        this.klass = klass;
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
    public String klass;
    public String method;
    public String filename;
    public int line;
}
