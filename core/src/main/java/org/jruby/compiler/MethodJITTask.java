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

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.ast.util.SexpMaker;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.MixedModeIRMethod;
import org.jruby.ir.targets.JVMVisitor;
import org.jruby.ir.targets.JVMVisitorMethodContext;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.collections.IntHashMap;

class MethodJITTask extends JITCompiler.Task {

    private final String className;
    private final MixedModeIRMethod method;
    private final String methodName;

    public MethodJITTask(JITCompiler jitCompiler, MixedModeIRMethod method, String className) {
        super(jitCompiler);
        this.method = method;
        this.className = className;
        this.methodName = method.getName();
    }

    @Override
    public void exec(ThreadContext context) throws NoSuchMethodException, IllegalAccessException {
        // Check if the method has been explicitly excluded
        String excludeModuleName = checkExcludedMethod(jitCompiler.config, className, methodName, method);
        if (excludeModuleName != null) {
            method.setCallCount(-1);
            if (jitCompiler.config.isJitLogging()) logImpl(context, "skipping method in " + excludeModuleName);
            return;
        }

        String key = SexpMaker.sha1(method.getIRScope());
        JVMVisitor visitor = JVMVisitor.newForJIT(context.runtime);
        MethodJITClassGenerator generator = new MethodJITClassGenerator(className, methodName, key, context.runtime, method, visitor);

        JVMVisitorMethodContext methodContext = new JVMVisitorMethodContext();
        generator.compile(methodContext);

        Class<?> sourceClass = defineClass(generator, visitor, method.getIRScope(), method.ensureInstrsReady());
        if (sourceClass == null) return; // class could not be found nor generated; give up on JIT and bail out

        String variableName = methodContext.getVariableName();
        MethodHandle variable = JITCompiler.PUBLIC_LOOKUP.findStatic(sourceClass, variableName, methodContext.getNativeSignature(-1));
        IntHashMap<MethodType> signatures = methodContext.getNativeSignaturesExceptVariable();

        if (signatures.size() == 0) {
            // only variable-arity
            method.completeBuild(context,
                    new CompiledIRMethod(
                            variable,
                            null,
                            -1,
                            method.getIRScope(),
                            method.getVisibility(),
                            method.getImplementationClass(),
                            ArgumentDescriptor.encode(method.getArgumentDescriptors())));

        } else {
            // also specific-arity
            for (IntHashMap.Entry<MethodType> entry : signatures.entrySet()) {
                method.completeBuild(context,
                        new CompiledIRMethod(
                                variable,
                                JITCompiler.PUBLIC_LOOKUP.findStatic(sourceClass, methodContext.getSpecificName(), entry.getValue()),
                                entry.getKey(),
                                method.getIRScope(),
                                method.getVisibility(),
                                method.getImplementationClass(),
                                ArgumentDescriptor.encode(method.getArgumentDescriptors())));
                break; // FIXME: only supports one arity
            }
        }
    }

    @Override
    protected String getSourceFile() {
        return method.getFile();
    }

    @Override
    protected void logJitted(ThreadContext context) {
        logImpl(context, "method done jitting");
    }

    @Override
    protected void logFailed(ThreadContext context, final Throwable ex) {
        logImpl(context, "could not compile method; passes run: " + method.getIRScope().getExecutedPasses(), ex);
    }

    @Override
    protected void logImpl(ThreadContext context, String message, Object... reason) {
        JITCompiler.log(context, method, methodName, message, reason);
    }

    static String checkExcludedMethod(final RubyInstanceConfig config, final String className, final String methodName,
                                      final Compilable target) {
        if (config.getExcludedMethods().size() > 0) {
            final RubyModule implementationClass = target.getImplementationClass();
            String excludeModuleName = className;
            if (implementationClass.getMethodLocation().isSingleton()) {
                RubyBasicObject possibleRealClass = ((MetaClass) implementationClass).getAttached();
                if (possibleRealClass instanceof RubyModule mod) {
                    excludeModuleName = "Meta:" + mod.getName(mod.getRuntime().getCurrentContext());
                }
            }

            if (config.getExcludedMethods().contains(excludeModuleName)
                    || config.getExcludedMethods().contains(methodName)
                    || config.getExcludedMethods().contains(excludeModuleName + '#' + methodName)
                    || config.getExcludedMethods().contains(target.getFile())
                    || config.getExcludedMethods().contains(target.getFile() + ':' + target.getLine())) {

                return excludeModuleName; // true - excluded
            }
        }
        return null;
    }

}
