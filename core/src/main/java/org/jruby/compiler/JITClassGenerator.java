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
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.targets.JVMVisitor;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.cli.Options;

public class JITClassGenerator {
    protected final String packageName;
    protected final String className;
    protected final String methodName;
    protected final String digestString;
    protected final JVMVisitor visitor;
    protected final String name;
    protected byte[] bytecode;
    protected long compileTime;

    public static final String CLASS_METHOD_DELIMITER = "$$";

    public JITClassGenerator(String className, String methodName, String key, Ruby ruby, JVMVisitor visitor) {
        this.methodName = methodName;
        this.packageName = JITCompiler.RUBY_JIT_PREFIX;
        this.visitor = visitor;
        if (Options.COMPILE_INVOKEDYNAMIC.load()) {
            // Some versions of Java 7 seems to have a bug that leaks definitions across cousin classloaders
            // so we force the class name to be unique to this runtime.

            // Also, invokedynamic forces us to make jitted bytecode unique to each runtime, since the call sites cache
            // at class level rather than at our runtime level. This makes it impossible to share jitted bytecode
            // across runtimes.

            digestString = key + Math.abs(ruby.hashCode());
        } else {
            digestString = key;
        }
        this.className = packageName + '/' + className.replace('.', '/') + CLASS_METHOD_DELIMITER + JavaNameMangler.mangleMethodName(methodName) + '_' + digestString;
        this.name = this.className.replace('/', '.');
    }

    void updateCounters(JITCounts counts, InterpreterContext ic) {
        counts.compiledCount.incrementAndGet();
        counts.compileTime.addAndGet(compileTime);
        counts.codeSize.addAndGet(bytecode.length);
        int insnCount = ic.getInstructions().length;
        counts.irSize.addAndGet(insnCount);
        counts.compileTimeAverage.set(counts.compileTime.get() / counts.compiledCount.get());
        counts.codeAverageSize.set(counts.codeSize.get() / counts.compiledCount.get());
        counts.irAverageSize.set(counts.irSize.get() / counts.compiledCount.get());
        synchronized (counts) {
            long largest;
            for (;;) {
                largest = counts.codeLargestSize.get();
                if (largest >= bytecode.length || counts.codeLargestSize.compareAndSet(largest, bytecode.length)) break;
            }
            for (;;) {
                largest = counts.irLargestSize.get();
                if (largest >= insnCount || counts.irLargestSize.compareAndSet(largest, insnCount)) break;
            }
        }
    }

    public byte[] bytecode() {
        return bytecode;
    }

    public String name() {
        return name;
    }
}
