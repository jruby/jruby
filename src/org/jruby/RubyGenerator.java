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
package org.jruby;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "Enumerator::Generator")
public class RubyGenerator extends RubyObject {
    private RubyProc proc;

    public static RubyClass createGeneratorClass(Ruby runtime) {
        RubyClass generatorc = runtime.defineClassUnder("Generator", runtime.getObject(), GENERATOR_ALLOCATOR, runtime.getEnumerator());
        runtime.setGenerator(generatorc);
        generatorc.index = ClassIndex.GENERATOR;
        generatorc.kindOf = new RubyModule.KindOf() {
            @Override
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyGenerator;
            }
        };

        generatorc.defineAnnotatedMethods(RubyGenerator.class);
        return generatorc;
    }

    private static ObjectAllocator GENERATOR_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyGenerator(runtime, klass);
        }
    };

    public RubyGenerator(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    public RubyGenerator(Ruby runtime) {
        super(runtime, runtime.getGenerator());
    }

    private void checkInit() {
        if (proc == null) throw getRuntime().newArgumentError("uninitializer generator");
    }

    private static RubyGenerator checkGenerator(IRubyObject obj) {
        if (!(obj instanceof RubyGenerator)) { 
            throw obj.getRuntime().newTypeError("wrong argument type " +
                    obj.getMetaClass().getRealClass().getName() + " (expected Data)"); 
        }
        RubyGenerator generator = (RubyGenerator)obj;
        generator.checkInit();
        return generator;
    }

    @JRubyMethod(name = "initialize", frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, Block block) {
        Ruby runtime = context.getRuntime();
        if (!block.isGiven()) throw runtime.newLocalJumpErrorNoBlock();
        proc = runtime.newProc(Block.Type.PROC, block);
        return this;
    }

    @JRubyMethod(name = "initialize", frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg, Block block) {
        Ruby runtime = context.getRuntime();
        if (!(arg instanceof RubyProc)) throw runtime.newThreadError("wrong argument type " + arg.getMetaClass() + " (expected Proc)");
        if (block.isGiven()) runtime.getWarnings().warning(ID.BLOCK_UNUSED, "given block not used");
        proc = (RubyProc)arg;
        return this;
    }

    @JRubyMethod(name = "initialize_copy", visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject obj) {
        proc = checkGenerator(obj).proc;
        return this;
    }

}
