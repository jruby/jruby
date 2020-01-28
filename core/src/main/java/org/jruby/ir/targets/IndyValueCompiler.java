package org.jruby.ir.targets;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.math.BigInteger;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

class IndyValueCompiler implements ValueCompiler {
    private IRBytecodeAdapter compiler;

    public IndyValueCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    public void pushRuntime() {
        compiler.loadContext();
        compiler.adapter.invokedynamic("runtime", sig(Ruby.class, ThreadContext.class), Bootstrap.contextValue());
    }

    public void pushObjectClass() {
        compiler.loadContext();
        compiler.invokeIRHelper("getObject", sig(RubyClass.class, ThreadContext.class));
    }

    public void pushUndefined() {
        compiler.adapter.getstatic(p(UndefinedValue.class), "UNDEFINED", ci(UndefinedValue.class));
    }

    public void pushFixnum(long l) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("fixnum", sig(JVM.OBJECT, ThreadContext.class), FixnumObjectSite.BOOTSTRAP, l);
    }

    public void pushFloat(double d) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("flote", sig(JVM.OBJECT, ThreadContext.class), FloatObjectSite.BOOTSTRAP, d);
    }

    public void pushString(ByteList bl, int cr) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("string", sig(RubyString.class, ThreadContext.class), Bootstrap.string(), RubyEncoding.decodeISO(bl), bl.getEncoding().toString(), cr);
    }

    public void pushFrozenString(ByteList bl, int cr, String file, int line) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("frozen", sig(RubyString.class, ThreadContext.class), Bootstrap.fstring(), RubyEncoding.decodeISO(bl), bl.getEncoding().toString(), cr, file, line);
    }

    public void pushByteList(ByteList bl) {
        compiler.adapter.invokedynamic("bytelist", sig(ByteList.class), Bootstrap.bytelist(), RubyEncoding.decodeISO(bl), bl.getEncoding().toString());
    }

    public void pushRegexp(ByteList source, int options) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("regexp", sig(RubyRegexp.class, ThreadContext.class), RegexpObjectSite.BOOTSTRAP, RubyEncoding.decodeISO(source), source.getEncoding().toString(), options);
    }

    public void pushSymbol(final ByteList bytes) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("symbol", sig(JVM.OBJECT, ThreadContext.class), SymbolObjectSite.BOOTSTRAP, RubyEncoding.decodeISO(bytes), bytes.getEncoding().toString());
    }

    public void pushSymbolProc(final ByteList bytes) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("symbolProc", sig(JVM.OBJECT, ThreadContext.class), SymbolProcObjectSite.BOOTSTRAP, RubyEncoding.decodeISO(bytes), bytes.getEncoding().toString());
    }

    public void pushEncoding(Encoding encoding) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("encoding", sig(RubyEncoding.class, ThreadContext.class), Bootstrap.contextValueString(), new String(encoding.getName()));
    }

    public void pushNil() {
        compiler.loadContext();
        compiler.adapter.invokedynamic("nil", sig(IRubyObject.class, ThreadContext.class), Bootstrap.contextValue());
    }

    public void pushBoolean(boolean b) {
        compiler.loadContext();
        compiler.adapter.invokedynamic(b ? "True" : "False", sig(IRubyObject.class, ThreadContext.class), Bootstrap.contextValue());
    }

    public void pushBignum(BigInteger bigint) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("bignum", sig(RubyBignum.class, ThreadContext.class), BignumObjectSite.BOOTSTRAP, bigint.toString());
    }
}
