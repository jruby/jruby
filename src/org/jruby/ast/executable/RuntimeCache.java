package org.jruby.ast.executable;

import java.math.BigInteger;
import java.util.Arrays;
import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyClass.VariableAccessor;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;

public class RuntimeCache {

    public RuntimeCache() {
    }

    public final StaticScope getScope(ThreadContext context, String varNamesDescriptor, int index) {
        StaticScope scope = scopes[index];
        if (scope == null) {
            scopes[index] = scope = RuntimeHelpers.createScopeForClass(context, varNamesDescriptor);
        }
        return scope;
    }

    public final StaticScope getScope(int index) {
        return scopes[index];
    }

    public final CallSite getCallSite(int index) {
        return callSites[index];
    }

    /**
     * descriptor format is
     *
     * closure_method_name,arity,varname1;varname2;varname3,has_multi_args_head,arg_type,light
     *
     * @param context
     * @param index
     * @param descriptor
     * @return
     */
    public final BlockBody getBlockBody(Object scriptObject, ThreadContext context, int index, String descriptor) {
        BlockBody body = blockBodies[index];
        if (body == null) {
            return createBlockBody(scriptObject, context, index, descriptor);
        }
        return body;
    }

    /**
     * descriptor format is
     *
     * closure_method_name,arity,varname1;varname2;varname3,has_multi_args_head,arg_type,light
     *
     * @param context
     * @param index
     * @param descriptor
     * @return
     */
    public final BlockBody getBlockBody19(Object scriptObject, ThreadContext context, int index, String descriptor) {
        BlockBody body = blockBodies[index];
        if (body == null) {
            return createBlockBody19(scriptObject, context, index, descriptor);
        }
        return body;
    }

    public final CompiledBlockCallback getBlockCallback(Object scriptObject, int index, String method) {
        CompiledBlockCallback callback = blockCallbacks[index];
        if (callback == null) {
            return createCompiledBlockCallback(scriptObject, index, method);
        }
        return callback;
    }

    public final RubySymbol getSymbol(ThreadContext context, int index, String name) {
        RubySymbol symbol = symbols[index];
        if (symbol == null) {
            return symbols[index] = context.runtime.newSymbol(name);
        }
        return symbol;
    }

    public final RubyString getString(ThreadContext context, int index, int codeRange) {
        return RubyString.newStringShared(context.runtime, getByteList(index), codeRange);
    }

    public final ByteList getByteList(int index) {
        return byteLists[index];
    }

    public final Encoding getEncoding(int index) {
        return encodings[index];
    }

    public final RubyFixnum getFixnum(ThreadContext context, int index, int value) {
        RubyFixnum fixnum = fixnums[index];
        if (fixnum == null) {
            return fixnums[index] = RubyFixnum.newFixnum(context.runtime, value);
        }
        return fixnum;
    }

    public final RubyFixnum getFixnum(ThreadContext context, int index, long value) {
        RubyFixnum fixnum = fixnums[index];
        if (fixnum == null) {
            return fixnums[index] = RubyFixnum.newFixnum(context.runtime, value);
        }
        return fixnum;
    }

    public final RubyFloat getFloat(ThreadContext context, int index, double value) {
        RubyFloat flote = floats[index];
        if (flote == null) {
            return floats[index] = RubyFloat.newFloat(context.runtime, value);
        }
        return flote;
    }

    public final RubyRegexp getRegexp(ThreadContext context, int index, ByteList pattern, int options) {
        RubyRegexp regexp = regexps[index];
        if (regexp == null || context.runtime.getKCode() != regexp.getKCode()) {
            regexp = RubyRegexp.newRegexp(context.runtime, pattern, RegexpOptions.fromEmbeddedOptions(options));
            regexp.setLiteral();
            regexps[index] = regexp;
        }
        return regexp;
    }

    public final RubyRegexp getRegexp(int index) {
        return regexps[index];
    }

    public final RubyRegexp cacheRegexp(int index, RubyString pattern, int options) {
        RubyRegexp regexp = regexps[index];
        Ruby runtime = pattern.getRuntime();
        if (regexp == null || runtime.getKCode() != regexp.getKCode()) {
            regexp = RubyRegexp.newRegexp(runtime, pattern.getByteList(), RegexpOptions.fromEmbeddedOptions(options));
            regexps[index] = regexp;
        }
        return regexp;
    }

    public final RubyRegexp cacheRegexp(int index, RubyRegexp regexp) {
        regexps[index] = regexp;
        return regexp;
    }

    public final BigInteger getBigInteger(int index, String pattern) {
        BigInteger bigint = bigIntegers[index];
        if (bigint == null) {
            return bigIntegers[index] = new BigInteger(pattern, 16);
        }
        return bigint;
    }

    public final IRubyObject getVariable(ThreadContext context, int index, String name, IRubyObject object) {
        IRubyObject value = getValue(context, index, name, object);
        if (value != null) return value;

        Ruby runtime = context.runtime;
        if (runtime.isVerbose()) {
            warnAboutUninitializedIvar(runtime, name);
        }
        return runtime.getNil();
    }

    public final IRubyObject getVariableDefined(ThreadContext context, int index, String name, IRubyObject object) {
        return getValue(context, index, name, object);
    }

    private final IRubyObject getValue(ThreadContext context, int index, String name, IRubyObject object) {
        VariableAccessor variableAccessor = variableReaders[index];
        RubyClass cls = object.getMetaClass().getRealClass();
        if (variableAccessor.getClassId() != cls.hashCode()) {
            variableReaders[index] = variableAccessor = cls.getVariableAccessorForRead(name);
        }
        return (IRubyObject)variableAccessor.get(object);
    }

    private void warnAboutUninitializedIvar(Ruby runtime, String name) {
        runtime.getWarnings().warning(ID.IVAR_NOT_INITIALIZED, "instance variable " + name + " not initialized");
    }

    public final IRubyObject setVariable(int index, String name, IRubyObject object, IRubyObject value) {
        VariableAccessor variableAccessor = variableWriters[index];
        RubyClass cls = object.getMetaClass().getRealClass();
        if (variableAccessor.getClassId() != cls.hashCode()) {
            variableWriters[index] = variableAccessor = cls.getVariableAccessorForWrite(name);
        }
        variableAccessor.set(object, value);
        return value;
    }

    public final void initScopes(int size) {
        scopes = new StaticScope[size];
    }

    public final void initCallSites(int size) {
        callSites = new CallSite[size];
    }

    /**
     * Given a packed descriptor listing methods and their type, populate the
     * call site cache.
     *
     * The format of the methods portion of the descriptor is
     * name1;type1;name2;type2 where type1 and type2 are a single capital letter
     * N, F, V, or S for the four main call types. After the method portion,
     * the other cache sizes are provided as a packed String of char values
     * representing the numeric sizes. @see RuntimeCache#initOthers.
     *
     * @param descriptor The descriptor to use for populating call sites and caches
     */
    public final void initFromDescriptor(String descriptor) {
        String[] pieces = descriptor.split("\uFFFF");
        CallSite[] sites = new CallSite[pieces.length - 1 / 2];

        // if there's no call sites, don't process it
        if (pieces[0].length() != 0) {
            for (int i = 0; i < pieces.length - 1; i+=2) {
                switch (pieces[i + 1].charAt(0)) {
                case 'N':
                    sites[i/2] = MethodIndex.getCallSite(pieces[i]);
                    break;
                case 'F':
                    sites[i/2] = MethodIndex.getFunctionalCallSite(pieces[i]);
                    break;
                case 'V':
                    sites[i/2] = MethodIndex.getVariableCallSite(pieces[i]);
                    break;
                case 'S':
                    sites[i/2] = MethodIndex.getSuperCallSite();
                    break;
                default:
                    throw new RuntimeException("Unknown call type: " + pieces[i + 1] + " for method " + pieces[i]);
                }
            }

            this.callSites = sites;
        }

        initOthers(pieces[pieces.length - 1]);
    }

    private static final int SCOPE = 0;
    private static final int SYMBOL = SCOPE + 1;
    private static final int FIXNUM = SYMBOL + 1;
    private static final int FLOAT = FIXNUM + 1;
    private static final int CONSTANT = FLOAT + 1;
    private static final int REGEXP = CONSTANT + 1;
    private static final int BIGINTEGER = REGEXP + 1;
    private static final int VARIABLEREADER = BIGINTEGER + 1;
    private static final int VARIABLEWRITER = VARIABLEREADER + 1;
    private static final int BLOCKBODY = VARIABLEWRITER + 1;
    private static final int BLOCKCALLBACK = BLOCKBODY + 1;
    private static final int METHOD = BLOCKCALLBACK + 1;
    private static final int STRING = METHOD + 1;
    private static final int ENCODING = STRING + 1;

    /**
     * Given a packed descriptor of other cache sizes, construct the cache arrays
     *
     * The format of the descriptor is the actual size cast to char in this order:
     * <ol>
       <li>scopeCount</li>
       <li>inheritedSymbolCount</li>
       <li>inheritedFixnumCount</li>
       <li>inheritedConstantCount</li>
       <li>inheritedRegexpCount</li>
       <li>inheritedBigIntegerCount</li>
       <li>inheritedVariableReaderCount</li>
       <li>inheritedVariableWriterCount</li>
       <li>inheritedBlockBodyCount</li>
       <li>inheritedBlockCallbackCount</li>
       <li>inheritedMethodCount</li>
       <li>inheritedStringCount</li>
     * </ul>
     *
     * @param descriptor The descriptor to use for preparing caches
     */
    public final void initOthers(String descriptor) {
        int scopeCount = getDescriptorValue(descriptor, SCOPE);
        if (scopeCount > 0) initScopes(scopeCount);
        int symbolCount = getDescriptorValue(descriptor, SYMBOL);
        if (symbolCount > 0) initSymbols(symbolCount);
        int fixnumCount = getDescriptorValue(descriptor, FIXNUM);
        if (fixnumCount > 0) initFixnums(fixnumCount);
        int floatCount = getDescriptorValue(descriptor, FLOAT);
        if (floatCount > 0) initFloats(floatCount);
        int constantCount = getDescriptorValue(descriptor, CONSTANT);
        if (constantCount > 0) initConstants(constantCount);
        int regexpCount = getDescriptorValue(descriptor, REGEXP);
        if (regexpCount > 0) initRegexps(regexpCount);
        int bigIntegerCount = getDescriptorValue(descriptor, BIGINTEGER);
        if (bigIntegerCount > 0) initBigIntegers(bigIntegerCount);
        int variableReaderCount = getDescriptorValue(descriptor, VARIABLEREADER);
        if (variableReaderCount > 0) initVariableReaders(variableReaderCount);
        int variableWriterCount = getDescriptorValue(descriptor, VARIABLEWRITER);
        if (variableWriterCount > 0) initVariableWriters(variableWriterCount);
        int blockBodyCount = getDescriptorValue(descriptor, BLOCKBODY);
        if (blockBodyCount > 0) initBlockBodies(blockBodyCount);
        int blockCallbackCount = getDescriptorValue(descriptor, BLOCKCALLBACK);
        if (blockCallbackCount > 0) initBlockCallbacks(blockCallbackCount);
        int methodCount = getDescriptorValue(descriptor, METHOD);
        if (methodCount > 0) initMethodCache(methodCount);
        int stringCount = getDescriptorValue(descriptor, STRING);
        if (stringCount > 0) initStrings(stringCount);
        int encodingCount = getDescriptorValue(descriptor, ENCODING);
        if (encodingCount > 0) initEncodings(encodingCount);
    }

    private static int getDescriptorValue(String descriptor, int type) {
        return descriptor.charAt(type);
    }

    public final void initBlockBodies(int size) {
        blockBodies = new BlockBody[size];
    }

    public final void initBlockCallbacks(int size) {
        blockCallbacks = new CompiledBlockCallback[size];
    }

    public final void initSymbols(int size) {
        symbols = new RubySymbol[size];
    }

    public final ByteList[] initStrings(int size) {
        return byteLists = new ByteList[size];
    }

    public final Encoding[] initEncodings(int size) {
        return encodings = new Encoding[size];
    }

    public final void initFixnums(int size) {
        fixnums = new RubyFixnum[size];
    }

    public final void initFloats(int size) {
        floats = new RubyFloat[size];
    }

    public final void initRegexps(int size) {
        regexps = new RubyRegexp[size];
    }

    public final void initBigIntegers(int size) {
        bigIntegers = new BigInteger[size];
    }

    public final void initConstants(int size) {
        constants = new IRubyObject[size];
        constantTargetHashes = new int[size];
        constantGenerations = new Object[size];
        Arrays.fill(constantGenerations, -1);
        Arrays.fill(constantTargetHashes, -1);
    }

    public final void initVariableReaders(int size) {
        variableReaders = new VariableAccessor[size];
        Arrays.fill(variableReaders, VariableAccessor.DUMMY_ACCESSOR);
    }

    public final void initVariableWriters(int size) {
        variableWriters = new VariableAccessor[size];
        Arrays.fill(variableWriters, VariableAccessor.DUMMY_ACCESSOR);
    }

    public final void initMethodCache(int size) {
        methodCache = new CacheEntry[size];
        Arrays.fill(methodCache, CacheEntry.NULL_CACHE);
    }

    public final IRubyObject getConstant(ThreadContext context, String name, int index) {
        IRubyObject value = getValue(context, name, index);
        // We can callsite cache const_missing if we want
        return value != null ? value : context.getCurrentScope().getStaticScope().getModule().callMethod(context, "const_missing", context.runtime.fastNewSymbol(name));
    }

    public IRubyObject getValue(ThreadContext context, String name, int index) {
        IRubyObject value = constants[index]; // Store to temp so it does null out on us mid-stream
        return isCached(context, value, index) ? value : reCache(context, name, index);
    }

    private boolean isCached(ThreadContext context, IRubyObject value, int index) {
        return value != null && constantGenerations[index] == context.runtime.getConstantInvalidator().getData();
    }

    public IRubyObject reCache(ThreadContext context, String name, int index) {
        Object newGeneration = context.runtime.getConstantInvalidator().getData();
        IRubyObject value = context.getConstant(name);
        constants[index] = value;
        if (value != null) {
            constantGenerations[index] = newGeneration;
        }
        return value;
    }

    public final IRubyObject getConstantFrom(RubyModule target, ThreadContext context, String name, int index) {
        IRubyObject value = getValueFrom(target, context, name, index);
        // We can callsite cache const_missing if we want
        return value != null ? value : target.getConstantFromConstMissing(name);
    }

    public IRubyObject getValueFrom(RubyModule target, ThreadContext context, String name, int index) {
        IRubyObject value = constants[index]; // Store to temp so it does null out on us mid-stream
        return isCachedFrom(target, context, value, index) ? value : reCacheFrom(target, context, name, index);
    }

    private boolean isCachedFrom(RubyModule target, ThreadContext context, IRubyObject value, int index) {
        return value != null && constantGenerations[index] == context.runtime.getConstantInvalidator().getData() && constantTargetHashes[index] == target.hashCode();
    }

    public IRubyObject reCacheFrom(RubyModule target, ThreadContext context, String name, int index) {
        Object newGeneration = context.runtime.getConstantInvalidator().getData();
        IRubyObject value = target.getConstantFromNoConstMissing(name, false);
        constants[index] = value;
        if (value != null) {
            constantGenerations[index] = newGeneration;
            constantTargetHashes[index] = target.hashCode();
        }
        return value;
    }

    private BlockBody createBlockBody(Object scriptObject, ThreadContext context, int index, String descriptor) throws NumberFormatException {
        BlockBody body = RuntimeHelpers.createCompiledBlockBody(context, scriptObject, descriptor);
        return blockBodies[index] = body;
    }

    private BlockBody createBlockBody19(Object scriptObject, ThreadContext context, int index, String descriptor) throws NumberFormatException {
        BlockBody body = RuntimeHelpers.createCompiledBlockBody19(context, scriptObject, descriptor);
        return blockBodies[index] = body;
    }

    private CompiledBlockCallback createCompiledBlockCallback(Object scriptObject, int index, String method) {
        CompiledBlockCallback callback = RuntimeHelpers.createBlockCallback(scriptObject, method, "(internal)", -1);
        return blockCallbacks[index] = callback;
    }

    public DynamicMethod getMethod(ThreadContext context, RubyClass selfType, int index, String methodName) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, selfType)) {
            return myCache.method;
        }
        return cacheAndGet(context, selfType, index, methodName);
    }

    public DynamicMethod getMethod(ThreadContext context, IRubyObject self, int index, String methodName) {
        return getMethod(context, pollAndGetClass(context, self), index, methodName);
    }

    private DynamicMethod cacheAndGet(ThreadContext context, RubyClass selfType, int index, String methodName) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (method.isUndefined()) {
            return RuntimeHelpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, CallType.FUNCTIONAL);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(RubyClass clazz, int index, String name1) {
        CacheEntry entry = clazz.searchWithCache(name1);
        DynamicMethod method = entry.method;
        if (entry.method == UndefinedMethod.INSTANCE) {
            return RuntimeHelpers.selectMethodMissing(clazz, method.getVisibility(), name1, CallType.FUNCTIONAL);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(RubyClass clazz, int index, String name1, String name2) {
        CacheEntry entry = clazz.searchWithCache(name1);
        DynamicMethod method = entry.method;
        if (entry.method == UndefinedMethod.INSTANCE) {
            return searchWithCache(clazz, index, name2);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(RubyClass clazz, int index, String name1, String name2, String name3) {
        CacheEntry entry = clazz.searchWithCache(name1);
        DynamicMethod method = entry.method;
        if (entry.method == UndefinedMethod.INSTANCE) {
            return searchWithCache(clazz, index, name2, name3);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(RubyClass clazz, int index, String name1, String name2, String name3, String name4) {
        CacheEntry entry = clazz.searchWithCache(name1);
        DynamicMethod method = entry.method;
        if (entry.method == UndefinedMethod.INSTANCE) {
            return searchWithCache(clazz, index, name2, name3, name4);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(RubyClass clazz, int index, String name1, String name2, String name3, String name4, String name5) {
        CacheEntry entry = clazz.searchWithCache(name1);
        DynamicMethod method = entry.method;
        if (entry.method == UndefinedMethod.INSTANCE) {
            return searchWithCache(clazz, index, name2, name3, name4, name5);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(RubyClass clazz, int index, String name1, String name2, String name3, String name4, String name5, String name6) {
        CacheEntry entry = clazz.searchWithCache(name1);
        DynamicMethod method = entry.method;
        if (entry.method == UndefinedMethod.INSTANCE) {
            return searchWithCache(clazz, index, name2, name3, name4, name5, name6);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(RubyClass clazz, int index, String name1, String name2, String name3, String name4, String name5, String name6, String name7) {
        CacheEntry entry = clazz.searchWithCache(name1);
        DynamicMethod method = entry.method;
        if (entry.method == UndefinedMethod.INSTANCE) {
            return searchWithCache(clazz, index, name2, name3, name4, name5, name6, name7);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(RubyClass clazz, int index, String name1, String name2, String name3, String name4, String name5, String name6, String name7, String name8) {
        CacheEntry entry = clazz.searchWithCache(name1);
        DynamicMethod method = entry.method;
        if (entry.method == UndefinedMethod.INSTANCE) {
            return searchWithCache(clazz, index, name2, name3, name4, name5, name6, name7, name8);
        }
        methodCache[index] = entry;
        return method;
    }

    public DynamicMethod searchWithCache(IRubyObject obj, int index, String name1) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, obj.getMetaClass())) {
            return myCache.method;
        }
        return searchWithCache(obj.getMetaClass(), index, name1);
    }

    public DynamicMethod searchWithCache(IRubyObject obj, int index, String name1, String name2) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, obj.getMetaClass())) {
            return myCache.method;
        }
        return searchWithCache(obj.getMetaClass(), index, name1, name2);
    }

    public DynamicMethod searchWithCache(IRubyObject obj, int index, String name1, String name2, String name3) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, obj.getMetaClass())) {
            return myCache.method;
        }
        return searchWithCache(obj.getMetaClass(), index, name1, name2, name3);
    }

    public DynamicMethod searchWithCache(IRubyObject obj, int index, String name1, String name2, String name3, String name4) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, obj.getMetaClass())) {
            return myCache.method;
        }
        return searchWithCache(obj.getMetaClass(), index, name1, name2, name3, name4);
    }

    public DynamicMethod searchWithCache(IRubyObject obj, int index, String name1, String name2, String name3, String name4, String name5) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, obj.getMetaClass())) {
            return myCache.method;
        }
        return searchWithCache(obj.getMetaClass(), index, name1, name2, name3, name4, name5);
    }

    public DynamicMethod searchWithCache(IRubyObject obj, int index, String name1, String name2, String name3, String name4, String name5, String name6) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, obj.getMetaClass())) {
            return myCache.method;
        }
        return searchWithCache(obj.getMetaClass(), index, name1, name2, name3, name4, name5, name6);
    }

    public DynamicMethod searchWithCache(IRubyObject obj, int index, String name1, String name2, String name3, String name4, String name5, String name6, String name7) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, obj.getMetaClass())) {
            return myCache.method;
        }
        return searchWithCache(obj.getMetaClass(), index, name1, name2, name3, name4, name5, name6, name7);
    }

    public DynamicMethod searchWithCache(IRubyObject obj, int index, String name1, String name2, String name3, String name4, String name5, String name6, String name7, String name8) {
        CacheEntry myCache = getCacheEntry(index);
        if (CacheEntry.typeOk(myCache, obj.getMetaClass())) {
            return myCache.method;
        }
        return searchWithCache(obj.getMetaClass(), index, name1, name2, name3, name4, name5, name6, name7, name8);
    }

    private static RubyClass pollAndGetClass(ThreadContext context, IRubyObject self) {
        context.callThreadPoll();
        RubyClass selfType = self.getMetaClass();
        return selfType;
    }

    private CacheEntry getCacheEntry(int index) {
        return methodCache[index];
    }

    private static final StaticScope[] EMPTY_SCOPES = {};
    public StaticScope[] scopes = EMPTY_SCOPES;
    private static final CallSite[] EMPTY_CALLSITES = {};
    public CallSite[] callSites = EMPTY_CALLSITES;
    private static final CacheEntry[] EMPTY_CACHEENTRIES = {};
    public CacheEntry[] methodCache = EMPTY_CACHEENTRIES;
    private static final BlockBody[] EMPTY_BLOCKBODIES = {};
    public BlockBody[] blockBodies = EMPTY_BLOCKBODIES;
    private static final CompiledBlockCallback[] EMPTY_COMPILEDBLOCKCALLBACKS = {};
    public CompiledBlockCallback[] blockCallbacks = EMPTY_COMPILEDBLOCKCALLBACKS;
    private static final RubySymbol[] EMPTY_RUBYSYMBOLS = {};
    public RubySymbol[] symbols = EMPTY_RUBYSYMBOLS;
    private static final ByteList[] EMPTY_BYTELISTS = {};
    public ByteList[] byteLists = EMPTY_BYTELISTS;
    private static final Encoding[] EMPTY_ENCODINGS = {};
    public Encoding[] encodings = EMPTY_ENCODINGS;
    private static final RubyFixnum[] EMPTY_FIXNUMS = {};
    public RubyFixnum[] fixnums = EMPTY_FIXNUMS;
    private static final RubyFloat[] EMPTY_FLOATS = {};
    public RubyFloat[] floats = EMPTY_FLOATS;
    private static final RubyRegexp[] EMPTY_RUBYREGEXPS = {};
    public RubyRegexp[] regexps = EMPTY_RUBYREGEXPS;
    private static final BigInteger[] EMPTY_BIGINTEGERS = {};
    public BigInteger[] bigIntegers = EMPTY_BIGINTEGERS;
    private static final VariableAccessor[] EMPTY_VARIABLE_ACCESSORS = {};
    public VariableAccessor[] variableReaders = EMPTY_VARIABLE_ACCESSORS;
    public VariableAccessor[] variableWriters = EMPTY_VARIABLE_ACCESSORS;
    public IRubyObject[] constants = IRubyObject.NULL_ARRAY;
    private static final int[] EMPTY_INTS = {};
    private static final Object[] EMPTY_OBJS = {};
    public Object[] constantGenerations = EMPTY_OBJS;
    public int[] constantTargetHashes = EMPTY_INTS;
}
