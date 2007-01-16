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
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
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
package org.jruby.ast.executable;

import java.io.Reader;
import java.io.IOException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.jruby.IRuby;
import org.jruby.RubyFile;
import org.jruby.RubyArray;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.parser.LocalStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class YARVCompiledRunner implements Runnable {
    private IRuby runtime;
    private YARVMachine ym;

    private YARVMachine.InstructionSequence iseq;

    public YARVCompiledRunner(IRuby runtime, Reader reader, String filename) {
        this.runtime = runtime;
        this.ym = new YARVMachine();
        char[] first = new char[4];
        try {
            reader.read(first);
            if(first[0] != 'R' || first[1] != 'B' || first[2] != 'C' || first[3] != 'M') {
                throw new RuntimeException("File is not a compiled YARV file");
            }
            RubyFile f = new RubyFile(runtime,filename,reader);
            IRubyObject arr = runtime.getModule("Marshal").callMethod(runtime.getCurrentContext(),"load",f);
            iseq = transformIntoSequence(arr);
        } catch(IOException e) {
            throw new RuntimeException("Couldn't read from source",e);
        }
    }

    public void run() {
        YARVMachine ym = new YARVMachine();
        ThreadContext context = runtime.getCurrentContext();
        StaticScope scope = new LocalStaticScope(null);
        scope.setVariables(iseq.locals);
        ym.exec(context, runtime.getObject(), scope, iseq.body);
    }

    private YARVMachine.InstructionSequence transformIntoSequence(IRubyObject arr) {
        if(!(arr instanceof RubyArray)) {
            throw new RuntimeException("Error when reading compiled YARV file");
        }

        YARVMachine.InstructionSequence seq = new YARVMachine.InstructionSequence();
        Iterator internal = (((RubyArray)arr).getList()).iterator();
        seq.magic = internal.next().toString();
        seq.major = RubyNumeric.fix2int((IRubyObject)internal.next());
        seq.minor = RubyNumeric.fix2int((IRubyObject)internal.next());
        seq.format_type = RubyNumeric.fix2int((IRubyObject)internal.next());
        IRubyObject misc = (IRubyObject)internal.next();
        if(misc.isNil()) {
            seq.misc = null;
        } else {
            seq.misc = misc;
        }
        seq.name = internal.next().toString();
        seq.filename = internal.next().toString();
        seq.line = new Object[0]; internal.next();
        seq.type = internal.next().toString();
        seq.locals = toStringArray((IRubyObject)internal.next());
        List arglist = ((RubyArray)internal.next()).getList();
        seq.args_argc = RubyNumeric.fix2int((IRubyObject)arglist.get(0));
        seq.args_arg_opts = RubyNumeric.fix2int((IRubyObject)arglist.get(1));
        seq.args_opt_labels = toStringArray((IRubyObject)arglist.get(2));
        seq.args_rest = RubyNumeric.fix2int((IRubyObject)arglist.get(3));
        seq.args_block = RubyNumeric.fix2int((IRubyObject)arglist.get(4));
        seq.exception = new Object[0]; internal.next();

        List bodyl = ((RubyArray)internal.next()).getList();
        YARVMachine.Instruction[] body = new YARVMachine.Instruction[bodyl.size()];
        int i=0;
        for(Iterator iter = bodyl.iterator();iter.hasNext();i++) {
            IRubyObject is = (IRubyObject)iter.next();
            if(is instanceof RubyArray) {
                body[i] = intoInstruction((RubyArray)is);
            } else if(is instanceof RubySymbol) {
                body[i] = intoLabel(is);
            }
        }
        seq.body = body;
        return seq;
    }

    private String[] toStringArray(IRubyObject obj) {
        if(obj.isNil()) {
            return new String[0];
        } else {
            List l = ((RubyArray)obj).getList();
            String[] s = new String[l.size()];
            int i=0;
            for(Iterator iter = l.iterator();iter.hasNext();i++) {
                s[i] = iter.next().toString();
            }
            return s;
        }
    }

    private YARVMachine.Instruction intoInstruction(RubyArray obj) {
        List internal = obj.getList();
        String name = internal.get(0).toString();
        int instruction = YARVMachine.instruction(name);
        YARVMachine.Instruction i = new YARVMachine.Instruction(instruction);
        if(internal.size() > 1) {
            IRubyObject first = (IRubyObject)internal.get(1);
            if(first instanceof RubyString) {
                i.s_op0 = first.toString();
            } else if(instruction == YARVInstructions.SEND) {
                i.s_op0 = first.toString();
                i.i_op1 = RubyNumeric.fix2int((IRubyObject)internal.get(2));
                i.i_op3 = RubyNumeric.fix2int((IRubyObject)internal.get(4));
            }
        }
        return i;
    }

    private YARVMachine.Instruction intoLabel(IRubyObject obj) {
        return null;
    }
}// YARVCompiledRunner
