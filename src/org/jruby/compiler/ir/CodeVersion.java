package org.jruby.compiler.ir;

public class CodeVersion {
    private static long _nextVersionNumber = 0L;

    // SSS FIXME: Does int suffice, or do we need long?
    public final long _version;

    public static CodeVersion getVersionToken() {
        _nextVersionNumber++;
        return new CodeVersion(_nextVersionNumber);
    }

    protected CodeVersion(long v) { _version = v; }

    static class ClassCodeVersion extends CodeVersion {
        ClassCodeVersion() { super(_nextVersionNumber+1); _nextVersionNumber++; }
        public String toString() { return "C_" + super.toString(); }
        private static long _nextVersionNumber = 0L;
    }

    static class MethodCodeVersion extends CodeVersion {
        MethodCodeVersion() { super(_nextVersionNumber+1); _nextVersionNumber++; }
        public String toString() { return "M_" + super.toString(); }
        private static long _nextVersionNumber = 0L;
    }
}
