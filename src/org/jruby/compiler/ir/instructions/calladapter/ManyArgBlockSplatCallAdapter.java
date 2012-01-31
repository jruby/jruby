/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.instructions.calladapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jruby.RubyArray;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Splat;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class ManyArgBlockSplatCallAdapter extends ManyArgBlockOperandCallAdapter {
    public ManyArgBlockSplatCallAdapter(CallSite callSite, Operand[] args, Operand closure) {
        super(callSite, args, closure);
    }
    
    @Override
    protected IRubyObject[] prepareArguments(ThreadContext context, IRubyObject self, Operand[] args, DynamicScope currDynScope, Object[] temp) {
        List<IRubyObject> argList = new ArrayList<IRubyObject>();
        int numArgs = args.length;
        for (int i = 0; i < numArgs; i++) {
            IRubyObject rArg = (IRubyObject) args[i].retrieve(context, self, currDynScope, temp);
            if (numArgs == 1 && args[i] instanceof Splat) {
                argList.addAll(Arrays.asList(((RubyArray)rArg).toJavaArray()));
            } else {
                argList.add(rArg);
            }
        }

        return argList.toArray(new IRubyObject[argList.size()]);
    }    
}
