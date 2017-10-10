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

import org.jruby.Ruby;
import org.jruby.internal.runtime.methods.MixedModeIRMethod;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.targets.JVMVisitor;
import org.jruby.ir.targets.JVMVisitorMethodContext;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.cli.Options;

public class MethodJITClassGenerator extends JITClassGenerator {
    public MethodJITClassGenerator(String className, String methodName, String key, Ruby ruby, MixedModeIRMethod method, JVMVisitor visitor) {
        super(className, methodName, key, ruby, visitor);
        this.method = method;
    }

    @SuppressWarnings("unchecked")
    protected void compile(JVMVisitorMethodContext context) {
        if (bytecode != null) return;

        // Time the compilation
        long start = System.nanoTime();

        InterpreterContext ic = method.ensureInstrsReady();

        int insnCount = ic.getInstructions().length;
        if (insnCount > Options.JIT_MAXSIZE.load()) {
            // methods with more than our limit of basic blocks are likely too large to JIT, so bail out
            throw new NotCompilableException("Could not compile " + method + "; instruction count " + insnCount + " exceeds threshold of " + Options.JIT_MAXSIZE.load());
        }

        // This may not be ok since we'll end up running passes specific to JIT
        // CON FIXME: Really should clone scope before passes in any case
        bytecode = visitor.compileToBytecode(method.getIRScope(), context);

//            try {
//                java.io.FileOutputStream fos = new java.io.FileOutputStream(className + '#' + methodName + ".class");
//                fos.write(bytecode);
//                fos.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

        compileTime = System.nanoTime() - start;
    }

    // FIXME: Does anything call this?  If so we should document it.
    public void generate() {
        compile(new JVMVisitorMethodContext());
    }

    @Override
    public String toString() {
        return methodName + "() at " + method.getFile() + ':' + method.getLine();
    }
    private final MixedModeIRMethod method;
}
