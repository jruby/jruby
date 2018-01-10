/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
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

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ast.util.SexpMaker;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.MixedModeIRMethod;
import org.jruby.ir.targets.JVMVisitor;
import org.jruby.ir.targets.JVMVisitorMethodContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.OneShotClassLoader;
import org.jruby.util.collections.IntHashMap;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

class MethodJITTask implements Runnable {
    private JITCompiler jitCompiler;
    private final String className;
    private final MixedModeIRMethod method;
    private final String methodName;

    public MethodJITTask(JITCompiler jitCompiler, MixedModeIRMethod method, String className) {
        this.jitCompiler = jitCompiler;
        this.method = method;
        this.className = className;
        this.methodName = method.getName();
    }

    public void run() {
        // We synchronize against the JITCompiler object so at most one code body will jit at once in a given runtime.
        // This works around unsolved concurrency issues within the process of preparing and jitting the IR.
        // See #4739 for a reproduction script that produced various errors without this.
        synchronized (jitCompiler) {
            try {
                // Check if the method has been explicitly excluded
                if (jitCompiler.config.getExcludedMethods().size() > 0) {
                    String excludeModuleName = className;
                    if (method.getImplementationClass().getMethodLocation().isSingleton()) {
                        IRubyObject possibleRealClass = ((MetaClass) method.getImplementationClass()).getAttached();
                        if (possibleRealClass instanceof RubyModule) {
                            excludeModuleName = "Meta:" + ((RubyModule) possibleRealClass).getName();
                        }
                    }

                    if ((jitCompiler.config.getExcludedMethods().contains(excludeModuleName)
                            || jitCompiler.config.getExcludedMethods().contains(excludeModuleName + '#' + methodName)
                            || jitCompiler.config.getExcludedMethods().contains(methodName))) {
                        method.setCallCount(-1);

                        if (jitCompiler.config.isJitLogging()) {
                            JITCompiler.log(method.getImplementationClass(), method.getFile(), method.getLine(), methodName, "skipping method: " + excludeModuleName + '#' + methodName);
                        }
                        return;
                    }
                }

                String key = SexpMaker.sha1(method.getIRScope());
                Ruby runtime = jitCompiler.runtime;
                JVMVisitor visitor = new JVMVisitor(runtime);
                MethodJITClassGenerator generator = new MethodJITClassGenerator(className, methodName, key, runtime, method, visitor);

                JVMVisitorMethodContext context = new JVMVisitorMethodContext();
                generator.compile(context);

                // FIXME: reinstate active bytecode size check
                // At this point we still need to reinstate the bytecode size check, to ensure we're not loading code
                // that's so big that JVMs won't even try to compile it. Removed the check because with the new IR JIT
                // bytecode counts often include all nested scopes, even if they'd be different methods. We need a new
                // mechanism of getting all method sizes.
                Class sourceClass = visitor.defineFromBytecode(method.getIRScope(), generator.bytecode(), new OneShotClassLoader(runtime.getJRubyClassLoader()));

                if (sourceClass == null) {
                    // class could not be found nor generated; give up on JIT and bail out
                    jitCompiler.counts.failCount.incrementAndGet();
                    return;
                } else {
                    generator.updateCounters(jitCompiler.counts, method.ensureInstrsReady());
                }

                // successfully got back a jitted method
                long methodCount = jitCompiler.counts.successCount.incrementAndGet();

                // logEvery n methods based on configuration
                if (jitCompiler.config.getJitLogEvery() > 0) {
                    if (methodCount % jitCompiler.config.getJitLogEvery() == 0) {
                        JITCompiler.log(method.getImplementationClass(), method.getFile(), method.getLine(), methodName, "live compiled methods: " + methodCount);
                    }
                }

                if (jitCompiler.config.isJitLogging()) {
                    JITCompiler.log(method.getImplementationClass(), method.getFile(), method.getLine(), className + '.' + methodName, "done jitting");
                }

                String variableName = context.getVariableName();
                MethodHandle variable = JITCompiler.PUBLIC_LOOKUP.findStatic(sourceClass, variableName, context.getNativeSignature(-1));
                IntHashMap<MethodType> signatures = context.getNativeSignaturesExceptVariable();

                if (signatures.size() == 0) {
                    // only variable-arity
                    method.completeBuild(
                            new CompiledIRMethod(
                                    variable,
                                    method.getIRScope(),
                                    method.getVisibility(),
                                    method.getImplementationClass(),
                                    method.getIRScope().receivesKeywordArgs()));

                } else {
                    // also specific-arity
                    for (IntHashMap.Entry<MethodType> entry : signatures.entrySet()) {
                        method.completeBuild(
                                new CompiledIRMethod(
                                        variable,
                                        JITCompiler.PUBLIC_LOOKUP.findStatic(sourceClass, context.getSpecificName(), entry.getValue()),
                                        entry.getKey(),
                                        method.getIRScope(),
                                        method.getVisibility(),
                                        method.getImplementationClass(),
                                        method.getIRScope().receivesKeywordArgs()));
                        break; // FIXME: only supports one arity
                    }
                }
            } catch (Throwable t) {
                if (jitCompiler.config.isJitLogging()) {
                    JITCompiler.log(method.getImplementationClass(), method.getFile(), method.getLine(), className + '.' + methodName, "Could not compile; passes run: " + method.getIRScope().getExecutedPasses(), t.getMessage());
                    if (jitCompiler.config.isJitLoggingVerbose()) {
                        t.printStackTrace();
                    }
                }

                jitCompiler.counts.failCount.incrementAndGet();
            }
        }
    }
}
