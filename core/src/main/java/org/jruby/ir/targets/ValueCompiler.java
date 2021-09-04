package org.jruby.ir.targets;

import org.jcodings.Encoding;
import org.jruby.ir.instructions.CallBase;
import org.jruby.util.ByteList;

import java.math.BigInteger;

public interface ValueCompiler {
    /**
     * Push the JRuby runtime on the stack.
     *
     * Stack required: none
     */
    public abstract void pushRuntime();

    /**
     * Push the Object class on the stack.
     *
     * Stack required: none
     */
    void pushObjectClass();

    /**
     * Push the UNDEFINED constant on the stack.
     *
     * Stack required: none
     */
    void pushUndefined();

    /**
     * Stack required: none
     *
     * @param l long value to push as a Fixnum
     */
    void pushFixnum(long l);

    /**
     * Stack required: none
     *
     * @param d double value to push as a Float
     */
    void pushFloat(double d);

    /**
     * Stack required: none
     *
     * @param bl ByteList for the String to push
     */
    void pushString(ByteList bl, int cr);

    /**
     * Stack required: none
     *
     * @param bl ByteList for the String to push
     */
    void pushFrozenString(ByteList bl, int cr, String path, int line);

    /**
     * Stack required: none
     *
     * @param bl ByteList to push
     */
    void pushByteList(ByteList bl);

    /**
     * Build and save a literal regular expression.
     * <p>
     * Stack required: none
     *
     * @param options options for the regexp
     */
    void pushRegexp(ByteList source, int options);

    /**
     * Push a symbol on the stack.
     *
     * Stack required: none
     *
     * @param bytes the ByteList for the symbol
     */
    void pushSymbol(ByteList bytes);

    /**
     * Push a Symbol.to_proc on the stack.
     *
     * Stack required: none
     *
     * @param bytes the ByteList for the symbol
     */
    void pushSymbolProc(ByteList bytes);

    /**
     * Push an encoding on the stack.
     *
     * Stack required: none
     *
     * @param encoding the encoding to push
     */
    void pushEncoding(Encoding encoding);

    /**
     * Load nil onto the stack.
     *
     * Stack required: none
     */
    void pushNil();

    /**
     * Load a boolean onto the stack.
     *
     * Stack required: none
     *
     * @param b the boolean to push
     */
    void pushBoolean(boolean b);

    /**
     * Load a Bignum onto the stack.
     *
     * Stack required: none
     *
     * @param bigint the value of the Bignum to push
     */
    void pushBignum(BigInteger bigint);

    /**
     * Load a CallSite onto the stack
     */
    void pushCallSite(String className, String siteName, String scopeFieldName, CallBase call);

    /**
     * Load a ConstantLookupSite onto the stack
     */
    void pushConstantLookupSite(String className, String siteName, ByteList name);

    /**
     * Push a new empty string on the stack
     *
     * Stack required: none
     */
    void pushEmptyString(Encoding encoding);
}
