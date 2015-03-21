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
 * Copyright (C) 2013 JRuby Contributors
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
package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyGenerator extends RubyObject {
    public static void createGeneratorClass(Ruby runtime) {
        RubyClass genc = runtime.defineClassUnder("Generator", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new RubyGenerator(runtime, klazz);
            }
        }, runtime.getEnumerator());

        genc.includeModule(runtime.getEnumerable());
        genc.defineAnnotatedMethods(RubyGenerator.class);

        runtime.setGenerator(genc);
    }

    public RubyGenerator(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    // generator_initialize
    @JRubyMethod(visibility = Visibility.PRIVATE, optional = 1)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;

        IRubyObject proc;

        if (args.length == 0) {
            proc = RubyProc.newProc(runtime, block, Block.Type.PROC);
        } else {
            if (!(args[0] instanceof RubyProc)) {
                throw runtime.newTypeError(args[0], runtime.getProc());
            }

            proc = args[0];

            if (block.isGiven()) {
                runtime.getWarnings().warn(IRubyWarnings.ID.BLOCK_UNUSED, "given block not used");
            }
        }

        return init(runtime, proc);
    }

    // generator_init_copy
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject other) {
        if (!(other instanceof RubyGenerator)) {
            throw context.runtime.newTypeError(other, context.runtime.getGenerator());
        }

        checkFrozen();

        this.proc = ((RubyGenerator)other).proc;

        return this;
    }

    // generator_each
    @JRubyMethod(rest = true)
    public IRubyObject each(ThreadContext context, IRubyObject[] args, Block block) {
        IRubyObject[] newArgs = new IRubyObject[args.length + 1];
        newArgs[0] = RubyYielder.newYielder(context, block);
        if (args.length > 0) System.arraycopy(args, 0, newArgs, 1, args.length);

        return ((RubyProc)proc).call19(context, newArgs, Block.NULL_BLOCK);
    }


    // generator_init
    private IRubyObject init(Ruby runtime, IRubyObject proc) {
        this.proc = proc;

        return this;
    }

    private IRubyObject proc;
}
