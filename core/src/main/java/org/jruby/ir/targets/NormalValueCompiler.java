package org.jruby.ir.targets;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyProc;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

class NormalValueCompiler implements ValueCompiler {
    private IRBytecodeAdapter compiler;

    public NormalValueCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    public void pushRuntime() {
        compiler.loadContext();
        compiler.adapter.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));
    }

    public void pushObjectClass() {
        compiler.loadContext();
        compiler.invokeIRHelper("getObject", sig(RubyClass.class, ThreadContext.class));
    }

    public void pushUndefined() {
        compiler.adapter.getstatic(p(UndefinedValue.class), "UNDEFINED", ci(UndefinedValue.class));
    }

    public void pushFixnum(final long l) {
        cacheValuePermanentlyLoadContext("fixnum", RubyFixnum.class, keyFor("fixnum", l), () -> {
            pushRuntime();
            compiler.adapter.ldc(l);
            compiler.adapter.invokevirtual(p(Ruby.class), "newFixnum", sig(RubyFixnum.class, long.class));
        });
    }

    public void pushFloat(final double d) {
        cacheValuePermanentlyLoadContext("float", RubyFloat.class, keyFor("float", Double.doubleToLongBits(d)), () -> {
            pushRuntime();
            compiler.adapter.ldc(d);
            compiler.adapter.invokevirtual(p(Ruby.class), "newFloat", sig(RubyFloat.class, double.class));
        });
    }

    public void pushString(ByteList bl, int cr) {
        pushRuntime();
        pushByteList(bl);
        compiler.adapter.ldc(cr);
        compiler.adapter.invokestatic(p(RubyString.class), "newStringShared", sig(RubyString.class, Ruby.class, ByteList.class, int.class));
    }

    public void pushFrozenString(final ByteList bl, final int cr, final String file, final int line) {
        cacheValuePermanentlyLoadContext("fstring", RubyString.class, keyFor("fstring", bl), () -> {
            compiler.loadContext();
            compiler.adapter.ldc(bl.toString());
            compiler.adapter.ldc(bl.getEncoding().toString());
            compiler.adapter.ldc(cr);
            compiler.adapter.ldc(file);
            compiler.adapter.ldc(line);
            compiler.invokeIRHelper("newFrozenStringFromRaw", sig(RubyString.class, ThreadContext.class, String.class, String.class, int.class, String.class, int.class));
        });
    }

    public void pushByteList(final ByteList bl) {
        cacheValuePermanentlyLoadContext("bytelist", ByteList.class, keyFor("bytelist", bl), () -> {
            pushRuntime();
            compiler.adapter.ldc(bl.toString());
            compiler.adapter.ldc(bl.getEncoding().toString());
            compiler.invokeIRHelper("newByteListFromRaw", sig(ByteList.class, Ruby.class, String.class, String.class));
        });
    }

    public void pushRegexp(final ByteList source, final int options) {
        cacheValuePermanentlyLoadContext("regexp", RubyRegexp.class, keyFor("regexp", source, options), () -> {
            compiler.loadContext();
            pushByteList(source);
            compiler.adapter.pushInt(options);
            compiler.invokeIRHelper("newLiteralRegexp", sig(RubyRegexp.class, ThreadContext.class, ByteList.class, int.class));
        });
    }

    public void pushSymbol(final ByteList bytes) {
        cacheValuePermanentlyLoadContext("symbol", RubySymbol.class, keyFor("symbol", bytes, bytes.getEncoding()), () -> {
            pushRuntime();
            pushByteList(bytes);
            compiler.adapter.invokestatic(p(RubySymbol.class), "newSymbol", sig(RubySymbol.class, Ruby.class, ByteList.class));
        });
    }

    public void pushSymbolProc(ByteList bytes) {
        cacheValuePermanentlyLoadContext("symbolProc", RubyProc.class, null, () -> {
            compiler.loadContext();
            pushByteList(bytes);
            compiler.invokeIRHelper("newSymbolProc", sig(RubyProc.class, ThreadContext.class, ByteList.class));
        });
    }

    public void pushEncoding(final Encoding encoding) {
        cacheValuePermanentlyLoadContext("encoding", RubySymbol.class, keyFor("encoding", encoding), () -> {
            compiler.loadContext();
            compiler.adapter.ldc(encoding.toString());
            compiler.invokeIRHelper("retrieveEncoding", sig(RubyEncoding.class, ThreadContext.class, String.class));
        });
    }

    public void pushNil() {
        compiler.loadContext();
        compiler.adapter.getfield(p(ThreadContext.class), "nil", ci(IRubyObject.class));
    }

    public void pushBoolean(boolean b) {
        pushRuntime();
        compiler.adapter.invokevirtual(p(Ruby.class), b ? "getTrue" : "getFalse", sig(RubyBoolean.class));
    }

    public void pushBignum(BigInteger bigint) {
        String bigintStr = bigint.toString();

        pushRuntime();
        compiler.adapter.ldc(bigintStr);
        compiler.adapter.invokestatic(p(RubyBignum.class), "newBignum", sig(RubyBignum.class, Ruby.class, String.class));
    }

    public String cacheValuePermanentlyLoadContext(String what, Class type, Object key, Runnable construction) {
        return cacheValuePermanently(what, type, key, false, sig(type, ThreadContext.class), compiler::loadContext, construction);
    }

    public String cacheValuePermanently(String what, Class type, Object key, boolean sync, Runnable construction) {
        return cacheValuePermanently(what, type, key, sync, sig(type), null, construction);
    }

    public String cacheValuePermanently(String what, Class type, Object key, boolean sync, String signature, Runnable loadState, Runnable construction) {
        String cacheName = key == null ? null : cacheFieldNames.get(key);
        String clsName = compiler.getClassData().clsName;

        if (cacheName == null) {
            cacheName = newFieldName(what);
            cacheFieldNames.put(key, cacheName);

            SkinnyMethodAdapter tmp = compiler.adapter;
            compiler.adapter = new SkinnyMethodAdapter(
                    compiler.adapter.getClassVisitor(),
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    cacheName,
                    signature,
                    null,
                    null);

            Label done = new Label();
            Label before = sync ? new Label() : null;
            Label after = sync ? new Label() : null;
            Label catchbody = sync ? new Label() : null;
            Label done2 = sync ? new Label() : null;

            compiler.adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, cacheName, ci(type), null, null).visitEnd();
            compiler.adapter.getstatic(clsName, cacheName, ci(type));
            compiler.adapter.dup();
            compiler.adapter.ifnonnull(done);
            compiler.adapter.pop();

            // lock class and check static field again
            Type classType = Type.getType("L" + clsName.replace('.', '/') + ';');
            int tempIndex = Type.getMethodType(signature).getArgumentsAndReturnSizes() >> 2 + 1;
            if (sync) {
                compiler.adapter.ldc(classType);
                compiler.adapter.dup();
                compiler.adapter.astore(tempIndex);
                compiler.adapter.monitorenter();

                compiler.adapter.trycatch(before, after, catchbody, null);

                compiler.adapter.label(before);
                compiler.adapter.getstatic(clsName, cacheName, ci(type));
                compiler.adapter.dup();
                compiler.adapter.ifnonnull(done2);
                compiler.adapter.pop();
            }

            construction.run();
            compiler.adapter.dup();
            compiler.adapter.putstatic(clsName, cacheName, ci(type));

            // unlock class along normal and exceptional exits
            if (sync) {
                compiler.adapter.label(done2);
                compiler.adapter.aload(tempIndex);
                compiler.adapter.monitorexit();
                compiler.adapter.go_to(done);
                compiler.adapter.label(after);

                compiler.adapter.label(catchbody);
                compiler.adapter.aload(tempIndex);
                compiler.adapter.monitorexit();
                compiler.adapter.athrow();
            }

            compiler.adapter.label(done);
            compiler.adapter.areturn();
            compiler.adapter.end();
            compiler.adapter = tmp;
        }

        if (loadState != null) loadState.run();

        compiler.adapter.invokestatic(clsName, cacheName, signature);

        return cacheName;
    }

    private String newFieldName(String baseName) {
        return baseName + compiler.getClassData().cacheFieldCount.getAndIncrement();
    }

    private static String keyFor(Object obj1, Object obj2) {
        StringBuilder sb = new StringBuilder(16);
        keyFor(sb, obj1);
        keyFor(sb, obj2);
        return sb.toString();
    }

    private static String keyFor(Object obj1, Object obj2, Object obj3) {
        StringBuilder sb = new StringBuilder(24);
        keyFor(sb, obj1);
        keyFor(sb, obj2);
        keyFor(sb, obj3);
        return sb.toString();
    }

    private static void keyFor(StringBuilder builder, Object obj) {
        builder.append(obj.toString());
        if (obj instanceof ByteList) builder.append('_').append(((ByteList) obj).getEncoding());
        builder.append('_');
    }

    private final Map<Object, String> cacheFieldNames = new HashMap<>();
}
