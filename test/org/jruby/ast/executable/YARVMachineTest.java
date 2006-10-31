package org.jruby.ast.executable;

import org.jruby.IRuby;
import org.jruby.Ruby;
import org.jruby.ast.executable.YARVMachine.Instruction;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import junit.framework.TestCase;

public class YARVMachineTest extends TestCase {
    public void testSimpleExecution() {
        YARVMachine ym = new YARVMachine();
        
        Instruction[] iseq = {
                new Instruction(YARVInstructions.PUTSTRING, new Object[] {"Hello, YARV!"}),
                new Instruction(YARVInstructions.DUP, null),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {Integer.valueOf(0)}),
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {Integer.valueOf(0)}),
                new Instruction(YARVInstructions.POP, null),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {Integer.valueOf(1)}),
                new Instruction(YARVInstructions.PUTOBJECT, new Object[] {Boolean.TRUE}),
                new Instruction(YARVInstructions.BRANCHIF, new Object[] {Integer.valueOf(10)}),
                new Instruction(YARVInstructions.PUTSTRING, new Object[] {"Wrong String"}),
                new Instruction(YARVInstructions.JUMP, new Object[] {Integer.valueOf(11)}),
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {Integer.valueOf(1)}),
                new Instruction(YARVInstructions.PUTOBJECT, new Object[] {Long.valueOf(2)}),
                new Instruction(YARVInstructions.SEND, new Object[] {"*", Integer.valueOf(1), null, Integer.valueOf(0)}),
                new Instruction(YARVInstructions.SEND, new Object[] {"to_s", Integer.valueOf(0), null, Integer.valueOf(YARVInstructions.VCALL_FLAG)}),
                new Instruction(YARVInstructions.SEND, new Object[] {"+", Integer.valueOf(1), null, Integer.valueOf(0)})
        };
        
        IRuby runtime = Ruby.newInstance(System.in, System.out, System.err);
        ThreadContext context = runtime.getCurrentContext();
        
        context.getFrameScope().addLocalVariables(new String[] {"zero", "one"});
        
        assertEquals("Hello, YARV!Hello, YARV!Object", ym.exec(context, runtime.getObject(), iseq).toString());
    }
    
    public void testIterativeFib() {
        YARVMachine ym = new YARVMachine();
        
        Instruction[] iseq = {
                // local var n declared (argument)
                new Instruction(YARVInstructions.PUTOBJECT, new Object[] {Long.valueOf(10)}), // fib index
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {Integer.valueOf(0)}),
                // method begins here
                // local var i declared
                new Instruction(YARVInstructions.PUTOBJECT, new Object[] {Long.valueOf(0)}),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {Integer.valueOf(1)}),
                // local var j declared
                new Instruction(YARVInstructions.PUTOBJECT, new Object[] {Long.valueOf(1)}),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {Integer.valueOf(2)}),
                // local var cur declared
                new Instruction(YARVInstructions.PUTOBJECT, new Object[] {Long.valueOf(1)}),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {Integer.valueOf(3)}),
                // while begins here, instruction 8
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {Integer.valueOf(3)}),
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {Integer.valueOf(0)}),
                new Instruction(YARVInstructions.SEND, new Object[] {"<=", Integer.valueOf(1), null, Integer.valueOf(0)}),
                new Instruction(YARVInstructions.BRANCHUNLESS, new Object[] {Integer.valueOf(25)}),
                // local var k declared, k = i
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {Integer.valueOf(1)}),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {Integer.valueOf(4)}),
                // i = j
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {Integer.valueOf(2)}),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {Integer.valueOf(1)}),
                // j = k + j
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {Integer.valueOf(4)}),
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {Integer.valueOf(2)}),
                new Instruction(YARVInstructions.SEND, new Object[] {"+", Integer.valueOf(1), null, Integer.valueOf(0)}),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {Integer.valueOf(2)}),
                // cur = cur + 1
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {Integer.valueOf(3)}),
                new Instruction(YARVInstructions.PUTOBJECT, new Object[] {Long.valueOf(1)}),
                new Instruction(YARVInstructions.SEND, new Object[] {"+", Integer.valueOf(1), null, Integer.valueOf(0)}),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {Integer.valueOf(3)}),
                // end while
                new Instruction(YARVInstructions.JUMP, new Object[] {Integer.valueOf(8)}),
                // return i, instruction 25
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {Integer.valueOf(1)})
        };
        
        IRuby runtime = Ruby.newInstance(System.in, System.out, System.err);
        ThreadContext context = runtime.getCurrentContext();
        
        context.getFrameScope().addLocalVariables(new String[] {"n", "i", "j", "cur", "k"});
        
        assertEquals("55", ym.exec(context, runtime.getObject(), iseq).toString());
        
        iseq[0] = new Instruction(YARVInstructions.PUTOBJECT, new Object[] {Long.valueOf(50000)});
        
        long time = System.currentTimeMillis();
        IRubyObject fib500k = ym.exec(context, runtime.getObject(), iseq);
        time = System.currentTimeMillis() - time;
        System.out.println(fib500k.toString());
        System.out.println("500000th fib in " + time + "ms");
    }
}
