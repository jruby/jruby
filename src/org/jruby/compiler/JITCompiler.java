/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.compiler;

import java.util.Set;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.Node;
import org.jruby.ast.executable.Script;
import org.jruby.ast.util.SexpMaker;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DefaultMethod;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.ClassCache;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.JavaNameMangler;

public class JITCompiler {
    public static final boolean USE_CACHE = true;
    
    public static void runJIT(final DefaultMethod method, final Ruby runtime, ThreadContext context, final String name) {
        Set<Script> jittedMethods = runtime.getJittedMethods();
        final RubyInstanceConfig instanceConfig = runtime.getInstanceConfig();
        int jitMax = instanceConfig.getJitMax();
        
        // We either are not JIT'ing, or we have already JIT'd too much.  Go no further.
        if (method.getCallCount() < 0 || jitMax == 0 || (jitMax != -1 && jittedMethods.size() >= jitMax)) return;
        
        try {
            method.setCallCount(method.getCallCount() + 1);

            if (method.getCallCount() >= instanceConfig.getJitThreshold()) {
                String cleanName = JavaNameMangler.mangleStringForCleanJavaIdentifier(name);
                Node bodyNode = method.getBodyNode();
                final ArgsNode argsNode = method.getArgsNode();
                String filename = calculateFilename(argsNode, bodyNode);
                StaticScope staticScope = method.getStaticScope();

                final StandardASMCompiler asmCompiler = new StandardASMCompiler(cleanName + 
                        method.hashCode() + "_" + context.hashCode(), filename);
                asmCompiler.startScript(staticScope);
                final ASTCompiler compiler = new ASTCompiler();

                CompilerCallback args = new CompilerCallback() {
                    public void call(MethodCompiler context) {
                        compiler.compileArgs(argsNode, context);
                    }
                };

                ASTInspector inspector = new ASTInspector();
                inspector.inspect(bodyNode);
                inspector.inspect(argsNode);

                MethodCompiler methodCompiler;
                CallConfiguration jitCallConfig = null;
                if (bodyNode != null) {
                    // we have a body, do a full-on method
                    methodCompiler = asmCompiler.startMethod("__file__", args, staticScope, inspector);
                    compiler.compile(bodyNode, methodCompiler);
                } else {
                    // If we don't have a body, check for required or opt args
                    // if opt args, they could have side effects
                    // if required args, need to raise errors if too few args passed
                    // otherwise, method does nothing, make it a nop
                    if (argsNode != null && (argsNode.getRequiredArgsCount() > 0 || argsNode.getOptionalArgsCount() > 0)) {
                        methodCompiler = asmCompiler.startMethod("__file__", args, staticScope, inspector);
                        methodCompiler.loadNil();
                    } else {
                        methodCompiler = asmCompiler.startMethod("__file__", null, staticScope, inspector);
                        methodCompiler.loadNil();
                        jitCallConfig = CallConfiguration.NO_FRAME_NO_SCOPE;
                    }
                }
                methodCompiler.endMethod();
                asmCompiler.endScript(false, false, false);

                ClassCache.ClassGenerator classGenerator = new ClassCache.ClassGenerator() {
                    @SuppressWarnings("unchecked")
                    public Class<Script> generate(ClassLoader classLoader) throws ClassNotFoundException {
                        Class<?> result = asmCompiler.loadClass(new JRubyClassLoader(classLoader));

                        if (instanceConfig.isJitLogging()) log(method, name, "compiled anew");

                        return (Class<Script>) result;
                    }
                };

                Class<Script> sourceClass;
                if (USE_CACHE) {
                    String key = SexpMaker.create(name, method.getArgsNode(), method.getBodyNode());

                    sourceClass = instanceConfig.getClassCache().cacheClassByKey(key, classGenerator);
                } else {
                    sourceClass = classGenerator.generate(new JRubyClassLoader(runtime.getJRubyClassLoader()));
                }

                // if we haven't already decided on a do-nothing call
                if (jitCallConfig == null) {
                    // if we're not doing any of the operations that still need
                    // a scope, use the scopeless config
                    if (inspector.hasClosure() || inspector.hasScopeAwareMethods()) {
                        jitCallConfig = CallConfiguration.FRAME_AND_SCOPE;
                    } else {
                        // switch to a slightly faster call config
                        jitCallConfig = CallConfiguration.FRAME_ONLY;
                    }
                }

                // finally, grab the script
                Script jitCompiledScript = (Script) sourceClass.newInstance();

                // add to the jitted methods set
                jittedMethods.add(jitCompiledScript);

                // logEvery n methods based on configuration
                if (instanceConfig.getJitLogEvery() > 0) {
                    int methodCount = jittedMethods.size();
                    if (methodCount % instanceConfig.getJitLogEvery() == 0) {
                        System.err.println("live compiled methods: " + methodCount);
                    }
                }

                if (instanceConfig.isJitLogging()) log(method, name, "done jitting");

                method.setJITCallConfig(jitCallConfig);
                method.setJITCompiledScript(jitCompiledScript);
                method.setCallCount(-1);
            }
        } catch (Exception e) {
            if (instanceConfig.isJitLoggingVerbose()) log(method, name, "could not compile", e.getMessage());

            method.setCallCount(-1);
        }
    }
    
    private static String calculateFilename(ArgsNode argsNode, Node bodyNode) {
        if (bodyNode != null) return bodyNode.getPosition().getFile();
        if (argsNode != null) return argsNode.getPosition().getFile();
        
        return "__eval__";
    }

    static void log(DefaultMethod method, String name, String message, String... reason) {
        String className = method.getImplementationClass().getBaseName();
        
        if (className == null) className = "<anon class>";

        System.err.print(message + ":" + className + "." + name);
        
        if (reason.length > 0) {
            System.err.print(" because of: \"");
            for (int i = 0; i < reason.length; i++) {
                System.err.print(reason[i]);
            }
            System.err.print('"');
        }
        
        System.err.println("");
    }
}
