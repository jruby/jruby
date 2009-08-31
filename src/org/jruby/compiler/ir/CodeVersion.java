package org.jruby.compiler.ir;

public class CodeVersion
{
    // SSS FIXME: Does int suffice, or do we need long?
    public final long _version;

    public static CodeVersion getVersionToken() { _nextVersionNumber++; return new CodeVersion(_nextVersionNumber); }

    public String toString() { return _version + "L"; }

    private CodeVersion(long v) { _version = v; }

    private static long _nextVersionNumber = 0L;
}
