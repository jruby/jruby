package org.jruby.ir.instructions.calladapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jruby.RubyArray;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Splat;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * AttrAssign is weird.  self[i] = 1 is treated as a functional call and
 * it also returns no result.
 */
public class AttrAssignCallAdapter extends CallAdapter {
    private String name;
    private Operand[] args;

    public AttrAssignCallAdapter(CallSite callSite, String name, Operand[] args) {
        super(callSite);

        this.args = args;
        this.name = name;
    }

    @Override
    public Object call(ThreadContext context, IRubyObject self, IRubyObject receiver, DynamicScope currDynScope, Object[] temp) {
        IRubyObject[] values = prepareArguments(context, self, args, currDynScope, temp);

        if (callSite == null) {
            CallType callType = self == receiver ? CallType.FUNCTIONAL : CallType.NORMAL;
            Helpers.invoke(context, receiver, name, values, callType, Block.NULL_BLOCK);
        } else {
            callSite.call(context, self, receiver, values);
        }

        return null;
    }

    protected IRubyObject[] prepareArguments(ThreadContext context, IRubyObject self, Operand[] args, DynamicScope currDynScope, Object[] temp) {
        List<IRubyObject> argList = new ArrayList<IRubyObject>();
        for (int i = 0; i < args.length; i++) {
            IRubyObject rArg = (IRubyObject) args[i].retrieve(context, self, currDynScope, temp);
            if (args[i] instanceof Splat) {
                argList.addAll(Arrays.asList(((RubyArray) rArg).toJavaArray()));
            } else {
                argList.add(rArg);
            }
        }

        return argList.toArray(new IRubyObject[argList.size()]);
    }
}
