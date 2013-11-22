/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006-2008 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2008 Thomas E Enebo <enebo@acm.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.compiler;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.MetaClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyInstanceConfig;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LambdaNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeType;
import org.jruby.ast.executable.AbstractScript;
import org.jruby.ast.executable.Script;
import org.jruby.ast.util.SexpMaker;
import org.jruby.compiler.impl.ChildScopedBodyCompiler;
import org.jruby.compiler.impl.ChildScopedBodyCompiler19;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.compiler.impl.StandardASMCompiler;
import static org.jruby.compiler.impl.StandardASMCompiler.ARGS_INDEX;
import static org.jruby.compiler.impl.StandardASMCompiler.SELF_INDEX;
import static org.jruby.compiler.impl.StandardASMCompiler.THIS;
import static org.jruby.compiler.impl.StandardASMCompiler.THREADCONTEXT_INDEX;
import static org.jruby.compiler.impl.StandardASMCompiler.getMethodSignature;
import static org.jruby.compiler.impl.StandardASMCompiler.getStaticMethodSignature;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DefaultMethod;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import static org.jruby.runtime.BlockBody.getArgumentTypeWackyHack;
import org.jruby.runtime.CompiledBlock;
import org.jruby.runtime.CompiledBlock19;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.CompiledBlockCallback19;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.threading.DaemonThreadFactory;
import org.jruby.util.ClassCache;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.cli.Options;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

public class JITCompiler implements JITCompilerMBean {
    private static final Logger LOG = LoggerFactory.getLogger("JITCompiler");
    
    public static final boolean USE_CACHE = true;
    public static final String RUBY_JIT_PREFIX = "rubyjit";

    public static final String CLASS_METHOD_DELIMITER = "$$";

    public static class JITCounts {
        private final AtomicLong compiledCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failCount = new AtomicLong(0);
        private final AtomicLong abandonCount = new AtomicLong(0);
        private final AtomicLong compileTime = new AtomicLong(0);
        private final AtomicLong averageCompileTime = new AtomicLong(0);
        private final AtomicLong codeSize = new AtomicLong(0);
        private final AtomicLong averageCodeSize = new AtomicLong(0);
        private final AtomicLong largestCodeSize = new AtomicLong(0);
    }

    private final JITCounts counts = new JITCounts();
    private final ExecutorService executor = new ThreadPoolExecutor(
                    2, // always two threads
                    2,
                    0, // never stop
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(),
                    new DaemonThreadFactory("JRubyJIT", Thread.MIN_PRIORITY));
    
    private final Ruby runtime;
    private final RubyInstanceConfig config;
    
    public JITCompiler(Ruby runtime) {
        this.runtime = runtime;
        this.config = runtime.getInstanceConfig();
        
        runtime.getBeanManager().register(this);
    }

    public long getSuccessCount() {
        return counts.successCount.get();
    }

    public long getCompileCount() {
        return counts.compiledCount.get();
    }

    public long getFailCount() {
        return counts.failCount.get();
    }

    public long getCompileTime() {
        return counts.compileTime.get() / 1000;
    }

    public long getAbandonCount() {
        return counts.abandonCount.get();
    }
    
    public long getCodeSize() {
        return counts.codeSize.get();
    }
    
    public long getAverageCodeSize() {
        return counts.averageCodeSize.get();
    }
    
    public long getAverageCompileTime() {
        return counts.averageCompileTime.get() / 1000;
    }
    
    public long getLargestCodeSize() {
        return counts.largestCodeSize.get();
    }

    public void tryJIT(DefaultMethod method, ThreadContext context, String className, String methodName) {
        if (!config.getCompileMode().shouldJIT()) return;
        
        if (method.incrementCallCount() < config.getJitThreshold()) return;
        
        jitThresholdReached(method, config, context, className, methodName);
    }

    public void tearDown() {
        if (executor != null) {
            try {
                executor.shutdown();
            } catch (SecurityException se) {
                // ignore, can't shut down executor
            }
        }
    }
    
    private void jitThresholdReached(final DefaultMethod method, final RubyInstanceConfig config, ThreadContext context, final String className, final String methodName) {
        // Disable any other jit tasks from entering queue
        method.setCallCount(-1);

        final Ruby runtime = context.runtime;
        
        Runnable jitTask = new JITTask(className, method, methodName);

        // if background JIT is enabled and threshold is > 0 and we have an executor...
        if (config.getJitBackground() &&
                config.getJitThreshold() > 0 &&
                executor != null) {
            // JIT in background
            try {
                executor.submit(jitTask);
            } catch (RejectedExecutionException ree) {
                // failed to submit, just run it directly
                jitTask.run();
            }
        } else {
            // just run directly
            jitTask.run();
        }
    }
    
    private class JITTask implements Runnable {
        private final String className;
        private final DefaultMethod method;
        private final String methodName;
        
        public JITTask(String className, DefaultMethod method, String methodName) {
            this.className = className;
            this.method = method;
            this.methodName = methodName;
        }
        
        public void run() {
            try {
                // The cache is full. Abandon JIT for this method and bail out.
                ClassCache classCache = config.getClassCache();
                if (classCache.isFull()) {
                    counts.abandonCount.incrementAndGet();
                    return;
                }

                // Check if the method has been explicitly excluded
                if (config.getExcludedMethods().size() > 0) {
                    String excludeModuleName = className;
                    if (method.getImplementationClass().isSingleton()) {
                        IRubyObject possibleRealClass = ((MetaClass) method.getImplementationClass()).getAttached();
                        if (possibleRealClass instanceof RubyModule) {
                            excludeModuleName = "Meta:" + ((RubyModule) possibleRealClass).getName();
                        }
                    }

                    if ((config.getExcludedMethods().contains(excludeModuleName)
                            || config.getExcludedMethods().contains(excludeModuleName + "#" + methodName)
                            || config.getExcludedMethods().contains(methodName))) {
                        method.setCallCount(-1);
                        log(method, methodName, "skipping method: " + excludeModuleName + "#" + methodName);
                        return;
                    }
                }

                String key = SexpMaker.create(methodName, method.getArgsNode(), method.getBodyNode());
                JITClassGenerator generator = new JITClassGenerator(className, methodName, key, runtime, method, counts);

                Class<Script> sourceClass = (Class<Script>) config.getClassCache().cacheClassByKey(generator.digestString, generator);

                if (sourceClass == null) {
                    // class could not be found nor generated; give up on JIT and bail out
                    counts.failCount.incrementAndGet();
                    return;
                }

                // successfully got back a jitted method
                counts.successCount.incrementAndGet();

                // finally, grab the script
                Script jitCompiledScript = sourceClass.newInstance();

                // set root scope
                jitCompiledScript.setRootScope(method.getStaticScope());

                // add to the jitted methods set
                Set<Script> jittedMethods = runtime.getJittedMethods();
                jittedMethods.add(jitCompiledScript);

                // logEvery n methods based on configuration
                if (config.getJitLogEvery() > 0) {
                    int methodCount = jittedMethods.size();
                    if (methodCount % config.getJitLogEvery() == 0) {
                        log(method, methodName, "live compiled methods: " + methodCount);
                    }
                }

                if (config.isJitLogging()) {
                    log(method, className + "." + methodName, "done jitting");
                }

                method.switchToJitted(jitCompiledScript, generator.callConfig());
                return;
            } catch (Throwable t) {
                if (runtime.getDebug().isTrue()) {
                    t.printStackTrace();
                }
                if (config.isJitLoggingVerbose()) {
                    log(method, className + "." + methodName, "could not compile", t.getMessage());
                }

                counts.failCount.incrementAndGet();
                return;
            }
        }
    }

    public static String getHashForString(String str) {
        return getHashForBytes(RubyEncoding.encodeUTF8(str));
    }

    public static String getHashForBytes(byte[] bytes) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            sha1.update(bytes);
            byte[] digest = sha1.digest();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                builder.append(Integer.toString( ( digest[i] & 0xff ) + 0x100, 16).substring( 1 ));
            }
            return builder.toString().toUpperCase(Locale.ENGLISH);
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }
    }

    public static void saveToCodeCache(Ruby ruby, byte[] bytecode, String packageName, File cachedClassFile) {
        String codeCache = RubyInstanceConfig.JIT_CODE_CACHE;
        File codeCacheDir = new File(codeCache);
        if (!codeCacheDir.exists()) {
            ruby.getWarnings().warn("jruby.jit.codeCache directory " + codeCacheDir + " does not exist");
        } else if (!codeCacheDir.isDirectory()) {
            ruby.getWarnings().warn("jruby.jit.codeCache directory " + codeCacheDir + " is not a directory");
        } else if (!codeCacheDir.canWrite()) {
            ruby.getWarnings().warn("jruby.jit.codeCache directory " + codeCacheDir + " is not writable");
        } else {
            if (!new File(codeCache, packageName).isDirectory()) {
                boolean createdDirs = new File(codeCache, packageName).mkdirs();
                if (!createdDirs) {
                    ruby.getWarnings().warn("could not create JIT cache dir: " + new File(codeCache, packageName));
                }
            }
            // write to code cache
            FileOutputStream fos = null;
            try {
                if (RubyInstanceConfig.JIT_LOADING_DEBUG) LOG.info("writing jitted code to to " + cachedClassFile);
                fos = new FileOutputStream(cachedClassFile);
                fos.write(bytecode);
            } catch (Exception e) {
                e.printStackTrace();
                // ignore
            } finally {
                try {fos.close();} catch (Exception e) {}
            }
        }
    }
    
    public static class JITClassGenerator implements ClassCache.ClassGenerator {
        public JITClassGenerator(String className, String methodName, String key, Ruby ruby, DefaultMethod method, JITCounts counts) {
            this.packageName = JITCompiler.RUBY_JIT_PREFIX;
            if (RubyInstanceConfig.JAVA_VERSION == Opcodes.V1_7 || Options.COMPILE_INVOKEDYNAMIC.load() == true) {
                // Some versions of Java 7 seems to have a bug that leaks definitions across cousin classloaders
                // so we force the class name to be unique to this runtime.

                // Also, invokedynamic forces us to make jitted code unique to each runtime, since the call sites cache
                // at class level rather than at our runtime level. This makes it impossible to share jitted code
                // across runtimes.
                
                digestString = getHashForString(key) + Math.abs(ruby.hashCode());
            } else {
                digestString = getHashForString(key);
            }
            this.className = packageName + "/" + className.replace('.', '/') + CLASS_METHOD_DELIMITER + JavaNameMangler.mangleMethodName(methodName) + "_" + digestString;
            this.name = this.className.replaceAll("/", ".");
            this.bodyNode = method.getBodyNode();
            this.argsNode = method.getArgsNode();
            this.methodName = methodName;
            filename = calculateFilename(argsNode, bodyNode);
            staticScope = method.getStaticScope();
            asmCompiler = new StandardASMCompiler(this.className, filename);
            this.ruby = ruby;
            this.counts = counts;
        }
        
        @SuppressWarnings("unchecked")
        protected void compile() {
            if (bytecode != null) return;
            
            // check if we have a cached compiled version on disk
            String codeCache = RubyInstanceConfig.JIT_CODE_CACHE;
            File cachedClassFile = new File(codeCache + "/" + className + ".class");

            if (codeCache != null &&
                    cachedClassFile.exists()) {
                FileInputStream fis = null;
                try {
                    if (RubyInstanceConfig.JIT_LOADING_DEBUG) LOG.info("loading cached code from: " + cachedClassFile);
                    fis = new FileInputStream(cachedClassFile);
                    bytecode = new byte[(int)fis.getChannel().size()];
                    fis.read(bytecode);
                    name = new ClassReader(bytecode).getClassName();
                    return;
                } catch (Exception e) {
                    // ignore and proceed to compile
                } finally {
                    try {fis.close();} catch (Exception e) {}
                }
            }
            
            // Time the compilation
            long start = System.nanoTime();

            asmCompiler.startScript(staticScope);
            final ASTCompiler compiler = ruby.getInstanceConfig().newCompiler();

            CompilerCallback args = new CompilerCallback() {
                public void call(BodyCompiler context) {
                    compiler.compileArgs(argsNode, context, true);
                }
            };

            ASTInspector inspector = new ASTInspector();
            if (ruby.getInstanceConfig().isJitDumping()) {
                inspector = new ASTInspector(className, true);
            }
            // check args first, since body inspection can depend on args
            inspector.inspect(argsNode);
            inspector.inspect(bodyNode);

            BodyCompiler methodCompiler;
            if (bodyNode != null) {
                // we have a body, do a full-on method
                methodCompiler = asmCompiler.startFileMethod(args, staticScope, inspector);
                compiler.compileBody(bodyNode, methodCompiler,true);
            } else {
                // If we don't have a body, check for required or opt args
                // if opt args, they could have side effects
                // if required args, need to raise errors if too few args passed
                // otherwise, method does nothing, make it a nop
                if (argsNode != null && (argsNode.getRequiredArgsCount() > 0 || argsNode.getOptionalArgsCount() > 0)) {
                    methodCompiler = asmCompiler.startFileMethod(args, staticScope, inspector);
                    methodCompiler.loadNil();
                } else {
                    methodCompiler = asmCompiler.startFileMethod(null, staticScope, inspector);
                    methodCompiler.loadNil();
                    jitCallConfig = CallConfiguration.FrameNoneScopeNone;
                }
            }
            methodCompiler.endBody();
            asmCompiler.endScript(false, false);
            
            // if we haven't already decided on a do-nothing call
            if (jitCallConfig == null) {
                jitCallConfig = inspector.getCallConfig();
            }
            
            bytecode = asmCompiler.getClassByteArray();
            if (ruby.getInstanceConfig().isJitDumping()) {
                TraceClassVisitor tcv = new TraceClassVisitor(new PrintWriter(System.out));
                new ClassReader(bytecode).accept(tcv, 0);
            }
            
            if (bytecode.length > ruby.getInstanceConfig().getJitMaxSize()) {
                bytecode = null;
                throw new NotCompilableException(
                        "JITed method size exceeds configured max of " +
                        ruby.getInstanceConfig().getJitMaxSize());
            }

            if (codeCache != null) {
                JITCompiler.saveToCodeCache(ruby, bytecode, packageName, cachedClassFile);
            }
            
            counts.compiledCount.incrementAndGet();
            counts.compileTime.addAndGet(System.nanoTime() - start);
            counts.codeSize.addAndGet(bytecode.length);
            counts.averageCompileTime.set(counts.compileTime.get() / counts.compiledCount.get());
            counts.averageCodeSize.set(counts.codeSize.get() / counts.compiledCount.get());
            synchronized (counts) {
                if (counts.largestCodeSize.get() < bytecode.length) {
                    counts.largestCodeSize.set(bytecode.length);
                }
            }
        }

        public void generate() {
            compile();
        }
        
        public byte[] bytecode() {
            return bytecode;
        }

        public String name() {
            return name;
        }
        
        public CallConfiguration callConfig() {
            compile();
            return jitCallConfig;
        }

        @Override
        public String toString() {
            return methodName + "() at " + bodyNode.getPosition().getFile() + ":" + bodyNode.getPosition().getLine();
        }
        
        private final StandardASMCompiler asmCompiler;
        private final StaticScope staticScope;
        private final Node bodyNode;
        private final ArgsNode argsNode;
        private final Ruby ruby;
        private final String packageName;
        private final String className;
        private final String filename;
        private final String methodName;
        private final JITCounts counts;
        private final String digestString;

        private CallConfiguration jitCallConfig;
        private byte[] bytecode;
        private String name;
    }

    public Block newCompiledClosure(ThreadContext context, IterNode iterNode, IRubyObject self) {
        Binding binding = context.currentBinding(self);
        NodeType argsNodeId = getArgumentTypeWackyHack(iterNode);

        boolean hasMultipleArgsHead = false;
        if (iterNode.getVarNode() instanceof MultipleAsgnNode) {
            hasMultipleArgsHead = ((MultipleAsgnNode) iterNode.getVarNode()).getHeadNode() != null;
        }

        BlockBody body = new CompiledBlock(Arity.procArityOf(iterNode.getVarNode()), iterNode.getScope(), compileBlock(context, new StandardASMCompiler("blahfooblah" + System.currentTimeMillis(), "blahfooblah"), iterNode), hasMultipleArgsHead, BlockBody.asArgumentType(argsNodeId));
        return new Block(body, binding);
    }

    public BlockBody newCompiledBlockBody(ThreadContext context, IterNode iterNode, Arity arity, int argumentType) {
        NodeType argsNodeId = getArgumentTypeWackyHack(iterNode);

        boolean hasMultipleArgsHead = false;
        if (iterNode.getVarNode() instanceof MultipleAsgnNode) {
            hasMultipleArgsHead = ((MultipleAsgnNode) iterNode.getVarNode()).getHeadNode() != null;
        }
        return new CompiledBlock(Arity.procArityOf(iterNode.getVarNode()), iterNode.getScope(), compileBlock(context, new StandardASMCompiler("blahfooblah" + System.currentTimeMillis(), "blahfooblah"), iterNode), hasMultipleArgsHead, BlockBody.asArgumentType(argsNodeId));
    }

    // ENEBO: Some of this logic should be put back into the Nodes themselves, but the more
    // esoteric features of 1.9 make this difficult to know how to do this yet.
    public BlockBody newCompiledBlockBody19(ThreadContext context, IterNode iterNode) {
        final ArgsNode argsNode = (ArgsNode)iterNode.getVarNode();

        boolean hasMultipleArgsHead = false;
        if (iterNode.getVarNode() instanceof MultipleAsgnNode) {
            hasMultipleArgsHead = ((MultipleAsgnNode) iterNode.getVarNode()).getHeadNode() != null;
        }

        NodeType argsNodeId = BlockBody.getArgumentTypeWackyHack(iterNode);
        
        return new CompiledBlock19(((ArgsNode)iterNode.getVarNode()).getArity(), iterNode.getScope(), compileBlock19(context, new StandardASMCompiler("blahfooblah" + System.currentTimeMillis(), "blahfooblah"), iterNode), hasMultipleArgsHead, BlockBody.asArgumentType(argsNodeId), Helpers.encodeParameterList(argsNode).split(";"));
    }
    
    public CompiledBlockCallback compileBlock(ThreadContext context, StandardASMCompiler asmCompiler, final IterNode iterNode) {
        final ASTCompiler astCompiler = new ASTCompiler();
        final StaticScope scope = iterNode.getScope();
        
        asmCompiler.startScript(scope);
        
        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        if (iterNode.getBodyNode() != null) {
                            astCompiler.compile(iterNode.getBodyNode(), context, true);
                        } else {
                            context.loadNil();
                        }
                    }
                };

        // create the closure class and instantiate it
        final CompilerCallback closureArgs = new CompilerCallback() {
            public void call(BodyCompiler context) {
                if (iterNode.getVarNode() != null) {
                    astCompiler.compileAssignment(iterNode.getVarNode(), context);
                } else {
                    context.consumeCurrentValue();
                }

                if (iterNode.getBlockVarNode() != null) {
                    astCompiler.compileAssignment(iterNode.getBlockVarNode(), context);
                } else {
                    context.consumeCurrentValue();
                }
            }
        };

        ASTInspector inspector = new ASTInspector();
        inspector.inspect(iterNode.getBodyNode());
        inspector.inspect(iterNode.getVarNode());
        
        int scopeIndex = asmCompiler.getCacheCompiler().reserveStaticScope();
        ChildScopedBodyCompiler closureCompiler = new ChildScopedBodyCompiler(asmCompiler, "__file__", asmCompiler.getClassname(), inspector, scope, scopeIndex);
        
        closureCompiler.beginMethod(closureArgs, scope);
        
        closureBody.call(closureCompiler);
        
        closureCompiler.endBody();
        
        // __file__ method with [] args; no-op
        SkinnyMethodAdapter method = new SkinnyMethodAdapter(asmCompiler.getClassVisitor(), ACC_PUBLIC, "__file__", getMethodSignature(4), null, null);
        method.start();

        method.aload(SELF_INDEX);
        method.areturn();
        method.end();
        
        // __file__ method to call static version
        method = new SkinnyMethodAdapter(asmCompiler.getClassVisitor(), ACC_PUBLIC, "__file__", getMethodSignature(1), null, null);
        method.start();

        // invoke static __file__
        method.aload(THIS);
        method.aload(THREADCONTEXT_INDEX);
        method.aload(SELF_INDEX);
        method.aload(ARGS_INDEX);
        method.aload(ARGS_INDEX + 1); // block
        method.invokestatic(asmCompiler.getClassname(), "__file__", getStaticMethodSignature(asmCompiler.getClassname(), 1));

        method.areturn();
        method.end();
        
        asmCompiler.endScript(false, false);
        
        byte[] bytes = asmCompiler.getClassByteArray();
        Class blockClass = new JRubyClassLoader(context.runtime.getJRubyClassLoader()).defineClass(asmCompiler.getClassname(), bytes);
        try {
            final AbstractScript script = (AbstractScript)blockClass.newInstance();
            script.setRootScope(scope);
            
            return new CompiledBlockCallback() {

                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject args, Block block) {
                    return script.__file__(context, self, args, block);
                }

                @Override
                public String getFile() {
                    return "blah";
                }

                @Override
                public int getLine() {
                    return -1;
                }
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public CompiledBlockCallback19 compileBlock19(ThreadContext context, StandardASMCompiler asmCompiler, final IterNode iterNode) {
        final ASTCompiler19 astCompiler = new ASTCompiler19();
        final StaticScope scope = iterNode.getScope();
        
        asmCompiler.startScript(scope);
        
        final ArgsNode argsNode = (ArgsNode)iterNode.getVarNode();

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {
            public void call(BodyCompiler context) {
                if (iterNode.getBodyNode() != null) {
                    astCompiler.compile(iterNode.getBodyNode(), context, true);
                } else {
                    context.loadNil();
                }
            }
        };

        // create the closure class and instantiate it
        final CompilerCallback closureArgs = new CompilerCallback() {
            public void call(BodyCompiler context) {
                // FIXME: This is temporary since the variable compilers assume we want
                // args already on stack for assignment. We just pop and continue with
                // 1.9 args logic.
                context.consumeCurrentValue(); // args value
                context.consumeCurrentValue(); // passed block
                if (iterNode.getVarNode() != null) {
                    if (iterNode instanceof LambdaNode) {
                        final int required = argsNode.getRequiredArgsCount();
                        final int opt = argsNode.getOptionalArgsCount();
                        final int rest = argsNode.getRestArg();
                        context.getVariableCompiler().checkMethodArity(required, opt, rest);
                        astCompiler.compileMethodArgs(argsNode, context, true);
                    } else {
                        astCompiler.compileMethodArgs(argsNode, context, true);
                    }
                }
            }
        };

        ASTInspector inspector = new ASTInspector();
        inspector.inspect(iterNode.getBodyNode());
        inspector.inspect(iterNode.getVarNode());
        
        NodeType argsNodeId = BlockBody.getArgumentTypeWackyHack(iterNode);
        
        int scopeIndex = asmCompiler.getCacheCompiler().reserveStaticScope();
        ChildScopedBodyCompiler closureCompiler = new ChildScopedBodyCompiler19(asmCompiler, "__file__", asmCompiler.getClassname(), inspector, scope, scopeIndex);
        
        closureCompiler.beginMethod(argsNodeId == null ? null : closureArgs, scope);
        
        closureBody.call(closureCompiler);
        
        closureCompiler.endBody();
        
        // __file__ method to call static version
        SkinnyMethodAdapter method = new SkinnyMethodAdapter(asmCompiler.getClassVisitor(), ACC_PUBLIC, "__file__", getMethodSignature(4), null, null);
        method.start();

        // invoke static __file__
        method.aload(THIS);
        method.aload(THREADCONTEXT_INDEX);
        method.aload(SELF_INDEX);
        method.aload(ARGS_INDEX);
        method.aload(ARGS_INDEX + 1); // block
        method.invokestatic(asmCompiler.getClassname(), "__file__", asmCompiler.getStaticMethodSignature(asmCompiler.getClassname(), 4));

        method.areturn();
        method.end();
        
        asmCompiler.endScript(false, false);
        
        byte[] bytes = asmCompiler.getClassByteArray();
        Class blockClass = new JRubyClassLoader(context.runtime.getJRubyClassLoader()).defineClass(asmCompiler.getClassname(), bytes);
        try {
            final AbstractScript script = (AbstractScript)blockClass.newInstance();
            script.setRootScope(scope);
            
            return new CompiledBlockCallback19() {

                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
                    return script.__file__(context, self, args, block);
                }

                @Override
                public String getFile() {
                    return iterNode.getPosition().getFile();
                }

                @Override
                public int getLine() {
                    return iterNode.getPosition().getLine();
                }
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void log(DefaultMethod method, String name, String message, String... reason) {
        String className = method.getImplementationClass().getBaseName();
        
        if (className == null) className = "<anon class>";

        StringBuilder builder = new StringBuilder(message + ":" + className + "." + name + " at " + method.getPosition());
        
        if (reason.length > 0) {
            builder.append(" because of: \"");
            for (int i = 0; i < reason.length; i++) {
                builder.append(reason[i]);
            }
            builder.append('"');
        }
        
        LOG.info(builder.toString());
    }
    
    private static String calculateFilename(ArgsNode argsNode, Node bodyNode) {
        if (bodyNode != null) return bodyNode.getPosition().getFile();
        if (argsNode != null) return argsNode.getPosition().getFile();
        
        return "__eval__";
    }
}
