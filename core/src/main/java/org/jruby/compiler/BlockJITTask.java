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

import org.jruby.ast.util.SexpMaker;
import org.jruby.ir.IRClosure;
import org.jruby.ir.targets.JVMVisitor;
import org.jruby.ir.targets.JVMVisitorMethodContext;
import org.jruby.runtime.CompiledIRBlockBody;
import org.jruby.runtime.MixedModeIRBlockBody;
import org.jruby.util.OneShotClassLoader;

class BlockJITTask implements Runnable {
    private JITCompiler jitCompiler;
    private final String className;
    private final MixedModeIRBlockBody body;
    private final String methodName;

    public BlockJITTask(JITCompiler jitCompiler, MixedModeIRBlockBody body, String className) {
        this.jitCompiler = jitCompiler;
        this.body = body;
        this.className = className;
        this.methodName = body.getName();
    }

    public void run() {
        // We synchronize against the JITCompiler object so at most one code body will jit at once in a given runtime.
        // This works around unsolved concurrency issues within the process of preparing and jitting the IR.
        // See #4739 for a reproduction script that produced various errors without this.
        synchronized (jitCompiler) {
            try {
                String key = SexpMaker.sha1(body.getIRScope());
                JVMVisitor visitor = new JVMVisitor(jitCompiler.runtime);
                BlockJITClassGenerator generator = new BlockJITClassGenerator(className, methodName, key, jitCompiler.runtime, body, visitor);

                JVMVisitorMethodContext context = new JVMVisitorMethodContext();
                generator.compile(context);

                // FIXME: reinstate active bytecode size check
                // At this point we still need to reinstate the bytecode size check, to ensure we're not loading code
                // that's so big that JVMs won't even try to compile it. Removed the check because with the new IR JIT
                // bytecode counts often include all nested scopes, even if they'd be different methods. We need a new
                // mechanism of getting all body sizes.
                Class sourceClass = visitor.defineFromBytecode(body.getIRScope(), generator.bytecode(), new OneShotClassLoader(jitCompiler.runtime.getJRubyClassLoader()));

                if (sourceClass == null) {
                    // class could not be found nor generated; give up on JIT and bail out
                    jitCompiler.counts.failCount.incrementAndGet();
                    return;
                } else {
                    generator.updateCounters(jitCompiler.counts, body.ensureInstrsReady());
                }

                // successfully got back a jitted body

                String jittedName = context.getVariableName();

                // blocks only have variable-arity
                body.completeBuild(
                        new CompiledIRBlockBody(
                                JITCompiler.PUBLIC_LOOKUP.findStatic(sourceClass, jittedName, JVMVisitor.CLOSURE_SIGNATURE.type()),
                                body.getIRScope(),
                                ((IRClosure) body.getIRScope()).getSignature().encode()));

                if (jitCompiler.config.isJitLogging()) {
                    JITCompiler.log(body.getImplementationClass(), body.getFile(), body.getLine(), className + "." + methodName, "done jitting");
                }
            } catch (Throwable t) {
                if (jitCompiler.config.isJitLogging()) {
                    JITCompiler.log(body.getImplementationClass(), body.getFile(), body.getLine(), className + "." + methodName, "Could not compile; passes run: " + body.getIRScope().getExecutedPasses(), t.getMessage());
                    if (jitCompiler.config.isJitLoggingVerbose()) {
                        t.printStackTrace();
                    }
                }

                jitCompiler.counts.failCount.incrementAndGet();
            }
        }
    }
}
