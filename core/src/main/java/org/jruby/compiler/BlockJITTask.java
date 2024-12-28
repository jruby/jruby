/*
 **** BEGIN LICENSE BLOCK *****
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

import org.jruby.Ruby;
import org.jruby.ast.util.SexpMaker;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRMethod;
import org.jruby.ir.targets.JVMVisitor;
import org.jruby.ir.targets.JVMVisitorMethodContext;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.CompiledIRBlockBody;
import org.jruby.runtime.MixedModeIRBlockBody;
import org.jruby.runtime.ThreadContext;

import static org.jruby.compiler.MethodJITTask.*;

class BlockJITTask extends JITCompiler.Task {

    private final String className;
    private final MixedModeIRBlockBody body;
    private final String blockId;
    private final String methodName;

    public BlockJITTask(JITCompiler jitCompiler, MixedModeIRBlockBody body, String className) {
        super(jitCompiler);
        this.body = body;
        this.className = className;
        this.blockId = body.getName();
        IRMethod method = body.getIRScope().getNearestMethod();
        this.methodName = method != null ? method.getByteName().toString() : null;
    }

    @Override
    public void exec(ThreadContext context) throws NoSuchMethodException, IllegalAccessException {
        // Check if the method has been explicitly excluded
        String excludeModuleName = checkExcludedMethod(jitCompiler.config, className, methodName, body);
        if (excludeModuleName != null) {
            body.setCallCount(-1);
            if (jitCompiler.config.isJitLogging()) {
                JITCompiler.log(body, blockId, "skipping block in " + excludeModuleName);
            }
            return;
        }

        IRClosure closure = body.getScope();
        StaticScope scope = closure.getStaticScope();
        final String key = SexpMaker.sha1(closure);
        final Ruby runtime = jitCompiler.runtime;
        JVMVisitor visitor = JVMVisitor.newForJIT(runtime);
        BlockJITClassGenerator generator = new BlockJITClassGenerator(className, blockId, key, runtime, body, visitor);

        JVMVisitorMethodContext methodContext = new JVMVisitorMethodContext();
        generator.compile(methodContext);

        Class<?> sourceClass = defineClass(generator, visitor, closure, body.ensureInstrsReady());
        if (sourceClass == null) return; // class could not be found nor generated; give up on JIT and bail out

        // successfully got back a jitted body
        String jittedName = methodContext.getVariableName();

        // blocks only have variable-arity
        body.completeBuild(context,
                new CompiledIRBlockBody(
                        JITCompiler.PUBLIC_LOOKUP.findStatic(sourceClass, jittedName, JVMVisitor.CLOSURE_SIGNATURE.type()),
                        scope,
                        closure.getFile(),
                        closure.getLine(),
                        ArgumentDescriptor.encode(closure.getArgumentDescriptors()),
                        ((IRClosure) body.getIRScope()).getSignature().encode()));
    }

    @Override
    protected String getSourceFile() {
        return body.getFile();
    }

    @Override
    protected void logJitted() {
        logImpl("block done jitting");
    }

    @Override
    protected void logFailed(final Throwable ex) {
        logImpl("could not compile block; passes run: " + body.getIRScope().getExecutedPasses(), ex);
    }

    @Override
    protected void logImpl(final String message, Object... reason) {
        JITCompiler.log(body, blockId, message, reason);
    }

}
