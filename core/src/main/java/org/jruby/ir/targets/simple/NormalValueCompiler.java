package org.jruby.ir.targets.simple;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyProc;
import org.jruby.RubyRange;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.ValueCompiler;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.MonomorphicCallSite;
import org.jruby.runtime.callsite.ProfilingCachingCallSite;
import org.jruby.runtime.callsite.RefinedCachingCallSite;
import org.jruby.runtime.callsite.VariableCachingCallSite;
import org.jruby.util.ByteList;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class NormalValueCompiler implements ValueCompiler {
    private final IRBytecodeAdapter compiler;

    public NormalValueCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    public void pushRuntime() {
        compiler.loadContext();
        compiler.adapter.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));
    }

    public void pushArrayClass() {
        compiler.loadContext();
        compiler.invokeIRHelper("getArray", sig(RubyClass.class, ThreadContext.class));
    }

    public void pushHashClass() {
        compiler.loadContext();
        compiler.invokeIRHelper("getHash", sig(RubyClass.class, ThreadContext.class));
    }

    public void pushObjectClass() {
        compiler.loadContext();
        compiler.invokeIRHelper("getObject", sig(RubyClass.class, ThreadContext.class));
    }

    public void pushSymbolClass() {
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

    public void pushChilledString(ByteList bl, int cr, String file, int line) {
        pushRuntime();
        pushByteList(bl);
        compiler.adapter.ldc(cr);
        compiler.adapter.ldc(file);
        compiler.adapter.ldc(line);
        compiler.adapter.invokestatic(p(RubyString.class), "newChilledString", sig(RubyString.class, Ruby.class, ByteList.class, int.class, String.class, int.class));
    }

    public void pushFrozenString(final ByteList bl, final int cr, final String file, final int line) {
        cacheValuePermanentlyLoadContext("fstring", RubyString.class, keyFor("fstring", bl), () -> {
            pushFrozenStringUncached(bl, cr, file, line);
        });
    }

    private void pushFrozenStringUncached(ByteList bl, int cr, String file, int line) {
        compiler.loadContext();
        compiler.adapter.ldc(bl.toString());
        compiler.adapter.ldc(bl.getEncoding().toString());
        compiler.adapter.ldc(cr);
        compiler.adapter.ldc(file);
        compiler.adapter.ldc(line);
        compiler.invokeIRHelper("newFrozenStringFromRaw", sig(RubyString.class, ThreadContext.class, String.class, String.class, int.class, String.class, int.class));
    }

    public void pushFrozenString(final ByteList bl, final int cr) {
        cacheValuePermanentlyLoadContext("fstring", RubyString.class, keyFor("fstring", bl), () -> {
            pushFrozenStringUncached(bl, cr);
        });
    }

    private void pushFrozenStringUncached(ByteList bl, int cr) {
        compiler.loadContext();
        compiler.adapter.ldc(bl.toString());
        compiler.adapter.ldc(bl.getEncoding().toString());
        compiler.adapter.ldc(cr);
        compiler.invokeIRHelper("newFrozenStringFromRaw", sig(RubyString.class, ThreadContext.class, String.class, String.class, int.class));
    }

    public void pushEmptyString(Encoding encoding) {
        pushRuntime();
        pushRubyEncoding(encoding);
        compiler.adapter.invokestatic(p(RubyString.class), "newEmptyString", sig(RubyString.class, Ruby.class, Encoding.class));
    }

    public void pushBufferString(Encoding encoding, int size) {
        pushRuntime();
        compiler.adapter.pushInt(size);
        pushEncoding(encoding);
        compiler.adapter.invokestatic(p(RubyString.class), "newStringLight", sig(RubyString.class, Ruby.class, int.class, Encoding.class));
    }

    public void buildDynamicString(Encoding encoding, int size, boolean frozen, boolean chilled, boolean debugFrozen, String file, int line, List<DStringElement> elements) {
        pushBufferString(encoding, size);

        for (DStringElement elt : elements) {
            switch (elt.type()) {
                case STRING:
                    StringLiteral str = (StringLiteral) elt.value();
                    pushFrozenString(str.getByteList(), str.getCodeRange());
                    compiler.adapter.invokevirtual(p(RubyString.class), "catWithCodeRange", sig(RubyString.class, RubyString.class));
                    break;
                case OTHER:
                    ((Runnable) elt.value()).run();
                    compiler.adapter.invokevirtual(p(RubyString.class), "appendAsDynamicString", sig(RubyString.class, IRubyObject.class));
            }
        }
        if (frozen) {
            compiler.invokeIRHelper("freezeLiteralString", sig(RubyString.class, RubyString.class));
        }

        if (chilled) {
            compiler.invokeIRHelper("chillLiteralString", sig(RubyString.class, RubyString.class));
        }
    }

    public void pushByteList(final ByteList bl) {
        cacheValuePermanentlyLoadContext("bytelist", ByteList.class, keyFor("bytelist", bl), () -> {
            pushRuntime();
            compiler.adapter.ldc(bl.toString());
            compiler.adapter.ldc(bl.getEncoding().toString());
            compiler.invokeIRHelper("newByteListFromRaw", sig(ByteList.class, Ruby.class, String.class, String.class));
        });
    }

    public void pushRange(Runnable begin, Runnable end, boolean exclusive) {
        cacheValuePermanentlyLoadContext("range", RubyRange.class, null, () -> {
            compiler.loadContext();
            begin.run();
            end.run();
            if (exclusive) {
                compiler.adapter.invokestatic(p(RubyRange.class), "newRange", sig(RubyRange.class, ThreadContext.class, IRubyObject.class, IRubyObject.class));
            } else {
                compiler.adapter.invokestatic(p(RubyRange.class), "newRange", sig(RubyRange.class, ThreadContext.class, IRubyObject.class, IRubyObject.class));
            }
        });
    }

    public void pushRange(long begin, long end, boolean exclusive) {
        cacheValuePermanentlyLoadContext("range", RubyRange.class, null, () -> {
            compiler.loadContext();
            compiler.adapter.ldc(begin);
            compiler.adapter.ldc(end);
            if (exclusive) {
                compiler.adapter.invokestatic(p(RubyRange.class), "newExclusiveRange", sig(RubyRange.class, ThreadContext.class, long.class, long.class));
            } else {
                compiler.adapter.invokestatic(p(RubyRange.class), "newInclusiveRange", sig(RubyRange.class, ThreadContext.class, long.class, long.class));
            }
        });
    }

    public void pushEndlessRange(long end, boolean exclusive) {
        cacheValuePermanentlyLoadContext("range", RubyRange.class, null, () -> {
            compiler.loadContext();
            compiler.adapter.ldc(end);
            compiler.adapter.ldc(exclusive);
            compiler.adapter.invokestatic(p(RubyRange.class), "newEndlessRange", sig(RubyRange.class, ThreadContext.class, long.class, boolean.class));
        });
    }

    public void pushBeginlessRange(long begin, boolean exclusive) {
        cacheValuePermanentlyLoadContext("range", RubyRange.class, null, () -> {
            compiler.loadContext();
            compiler.adapter.ldc(begin);
            compiler.adapter.ldc(exclusive);
            compiler.adapter.invokestatic(p(RubyRange.class), "newBeginlessRange", sig(RubyRange.class, ThreadContext.class, long.class, boolean.class));
        });
    }

    public void pushRange(ByteList begin, int beginCR, ByteList end, int endCR, boolean exclusive) {
        cacheValuePermanentlyLoadContext("range", RubyRange.class, null, () -> {
            compiler.loadContext();
            pushFrozenStringUncached(begin, beginCR);
            pushFrozenStringUncached(end, endCR);
            if (exclusive) {
                compiler.adapter.invokestatic(p(RubyRange.class), "newExclusiveRange", sig(RubyRange.class, ThreadContext.class, IRubyObject.class, IRubyObject.class));
            } else {
                compiler.adapter.invokestatic(p(RubyRange.class), "newInclusiveRange", sig(RubyRange.class, ThreadContext.class, IRubyObject.class, IRubyObject.class));
            }
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

    public void pushRubyEncoding(final Encoding encoding) {
        cacheValuePermanentlyLoadContext("rubyEncoding", RubyEncoding.class, keyFor("rubyEncoding", encoding), () -> {
            compiler.loadContext();
            compiler.adapter.ldc(encoding.toString());
            compiler.invokeIRHelper("retrieveEncoding", sig(RubyEncoding.class, ThreadContext.class, String.class));
        });
    }

    public void pushEncoding(final Encoding encoding) {
        cacheValuePermanentlyLoadContext("encoding", Encoding.class, keyFor("encoding", encoding), () -> {
            compiler.loadContext();
            compiler.adapter.ldc(encoding.toString());
            compiler.invokeIRHelper("retrieveJCodingsEncoding", sig(Encoding.class, ThreadContext.class, String.class));
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

    @Override
    public void pushCallSite(String className, String siteName, String scopeFieldName, CallBase call) {
        CallType callType = call.getCallType();
        boolean profileCandidate = call.hasLiteralClosure() && scopeFieldName != null && IRManager.IR_INLINER;
        boolean profiled = false;
        boolean refined = call.isPotentiallyRefined();

        SkinnyMethodAdapter method = compiler.adapter;

        // site requires special handling (usually refined or profiled that need scope present)
        Class<? extends CachingCallSite> siteClass;
        String signature;

        // call site object field
        method.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, siteName, ci(CachingCallSite.class), null, null).visitEnd();

        // lazily construct it
        method.getstatic(className, siteName, ci(CachingCallSite.class));
        method.dup();
        Label doCall = new Label();
        method.ifnonnull(doCall);
        method.pop();
        method.ldc(call.getId());
        if (refined) {
            siteClass = RefinedCachingCallSite.class;
            signature = sig(siteClass, String.class, StaticScope.class, String.class);
            method.getstatic(className, scopeFieldName, ci(StaticScope.class));
            method.ldc(callType.name());
        } else {
            switch (callType) {
                case NORMAL:
                    if (profileCandidate) {
                        profiled = true;
                        siteClass = ProfilingCachingCallSite.class;
                    } else {
                        siteClass = MonomorphicCallSite.class;
                    }
                    break;
                case FUNCTIONAL:
                    if (profileCandidate) {
                        profiled = true;
                        siteClass = ProfilingCachingCallSite.class;
                    } else {
                        siteClass = FunctionalCachingCallSite.class;
                    }
                    break;
                case VARIABLE:
                    siteClass = VariableCachingCallSite.class;
                    break;
                default:
                    throw new RuntimeException("BUG: Unexpected call type " + callType + " in JVM6 invoke logic");
            }
            if (profiled) {
                method.getstatic(className, scopeFieldName, ci(IRScope.class));
                method.ldc(call.getCallSiteId());
                signature = sig(CallType.class, siteClass, String.class, IRScope.class, long.class);
            } else {
                signature = sig(siteClass, String.class);
            }
        }
        method.invokestatic(p(IRRuntimeHelpers.class), "new" + siteClass.getSimpleName(), signature);
        method.dup();
        method.putstatic(className, siteName, ci(CachingCallSite.class));

        method.label(doCall);
    }

    @Override
    public void pushConstantLookupSite(String className, String siteName, ByteList name) {
        SkinnyMethodAdapter method = compiler.adapter;

        // constant lookup site object field
        method.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, siteName, ci(ConstantLookupSite.class), null, null).visitEnd();

        // lazily construct it
        method.getstatic(className, siteName, ci(ConstantLookupSite.class));
        method.dup();
        Label doLookup = new Label();
        method.ifnonnull(doLookup);
        method.pop();
        method.newobj(p(ConstantLookupSite.class));
        method.dup();
        pushSymbol(name);
        method.invokespecial(p(ConstantLookupSite.class), "<init>", sig(void.class, RubySymbol.class));
        method.dup();
        method.putstatic(className, siteName, ci(ConstantLookupSite.class));

        method.label(doLookup);
    }

    public String cacheValuePermanentlyLoadContext(String what, Class type, Object key, Runnable construction) {
        return cacheValuePermanently(what, type, key, false, MethodType.methodType(type, ThreadContext.class), compiler::loadContext, construction);
    }

    public String cacheValuePermanently(String what, Class type, Object key, boolean sync, Runnable construction) {
        return cacheValuePermanently(what, type, key, sync, MethodType.methodType(type), null, construction);
    }

    public String cacheValuePermanently(String what, Class type, Object key, boolean sync, MethodType signature, Runnable loadState, Runnable construction) {
        String cacheName = key == null ? null : cacheFieldNames.get(key);
        String clsName = compiler.getClassData().clsName;

        if (cacheName == null) {
            final String newCacheName = cacheName = newFieldName(what);
            cacheFieldNames.put(key, newCacheName);

            compiler.outline(newCacheName, signature, () -> {
                Label done = new Label();
                Label before = sync ? new Label() : null;
                Label after = sync ? new Label() : null;
                Label catchbody = sync ? new Label() : null;
                Label done2 = sync ? new Label() : null;

                compiler.adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, newCacheName, ci(type), null, null).visitEnd();
                compiler.adapter.getstatic(clsName, newCacheName, ci(type));
                compiler.adapter.dup();
                compiler.adapter.ifnonnull(done);
                compiler.adapter.pop();

                // lock class and check static field again
                Type classType = Type.getType("L" + clsName.replace('.', '/') + ';');
                int tempIndex = Type.getMethodType(sig(signature)).getArgumentsAndReturnSizes() >> 2 + 1;
                if (sync) {
                    compiler.adapter.ldc(classType);
                    compiler.adapter.dup();
                    compiler.adapter.astore(tempIndex);
                    compiler.adapter.monitorenter();

                    compiler.adapter.trycatch(before, after, catchbody, null);

                    compiler.adapter.label(before);
                    compiler.adapter.getstatic(clsName, newCacheName, ci(type));
                    compiler.adapter.dup();
                    compiler.adapter.ifnonnull(done2);
                    compiler.adapter.pop();
                }

                construction.run();
                compiler.adapter.dup();
                compiler.adapter.putstatic(clsName, newCacheName, ci(type));

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
            });
        }

        if (loadState != null) loadState.run();

        compiler.adapter.invokestatic(clsName, cacheName, sig(signature));

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

    public void pushFixnumArray(List<Long> values) {
        values.forEach(obj -> pushFixnum(obj.longValue()));

        compiler.getDynamicValueCompiler().array(values.size());
    }

    public void pushFloatArray(List<Double> values) {
        values.forEach(obj -> pushFloat(obj.doubleValue()));

        compiler.getDynamicValueCompiler().array(values.size());
    }
}
