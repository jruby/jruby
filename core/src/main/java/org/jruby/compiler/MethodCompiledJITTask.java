/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import org.jruby.Ruby;
import org.jruby.ast.util.SexpMaker;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.ir.targets.JVMVisitor;
import org.jruby.ir.targets.JVMVisitorMethodContext;
import org.jruby.util.OneShotClassLoader;
import org.jruby.util.collections.IntHashMap;

import static org.jruby.compiler.MethodJITTask.*;

/**
 * Created by enebo on 10/30/18.
 */
class MethodCompiledJITTask extends JITCompiler.Task {

    private final String className;
    private final CompiledIRMethod method;
    private final String methodName;

    public MethodCompiledJITTask(JITCompiler jitCompiler, CompiledIRMethod method, String className) {
        super(jitCompiler);
        this.method = method;
        this.className = className;
        this.methodName = method.getName();
    }

    @Override
    public void exec() {
        try {
            // Check if the method has been explicitly excluded
            String excludeModuleName = checkExcludedMethod(jitCompiler.config, className, methodName, method);
            if (excludeModuleName != null) {
                method.setCallCount(-1);
                if (jitCompiler.config.isJitLogging()) {
                    JITCompiler.log(method.getImplementationClass(), method.getFile(), method.getLine(), methodName, "skipping method: " + excludeModuleName + '#' + methodName);
                }
                return;
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

            method.setVariable(variable);
            if (signatures.size() != 0) {
                for (IntHashMap.Entry<MethodType> entry : signatures.entrySet()) {
                    method.setSpecific(JITCompiler.PUBLIC_LOOKUP.findStatic(sourceClass, context.getSpecificName(), entry.getValue()));
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
