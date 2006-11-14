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
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {new Integer(0)}),
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {new Integer(0)}),
                new Instruction(YARVInstructions.POP, null),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {new Integer(1)}),
                new Instruction(YARVInstructions.PUTOBJECT, new Object[] {Boolean.TRUE}),
                new Instruction(YARVInstructions.BRANCHIF, new Object[] {new Integer(10)}),
                new Instruction(YARVInstructions.PUTSTRING, new Object[] {"Wrong String"}),
                new Instruction(YARVInstructions.JUMP, new Object[] {new Integer(11)}),
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {new Integer(1)}),
                new Instruction(YARVInstructions.PUTOBJECT, new Object[] {new Long(2)}),
                new Instruction(YARVInstructions.SEND, new Object[] {"*", new Integer(1), null, new Integer(0)}),
                new Instruction(YARVInstructions.SEND, new Object[] {"to_s", new Integer(0), null, new Integer(YARVInstructions.VCALL_FLAG)}),
                new Instruction(YARVInstructions.SEND, new Object[] {"+", new Integer(1), null, new Integer(0)})
        };
        
        IRuby runtime = Ruby.newInstance(System.in, System.out, System.err);
        ThreadContext context = runtime.getCurrentContext();
        
        //context.getFrameScope().addLocalVariables(new String[] {"zero", "one"});
        
        assertEquals("Hello, YARV!Hello, YARV!Object", ym.exec(context, runtime.getObject(), iseq).toString());
    }
    
    public void testIterativeFib() {
        YARVMachine ym = new YARVMachine();
        
        Instruction[] iseq = {
                // local var n declared (argument)
                new Instruction(YARVInstructions.PUTOBJECT, new Object[] {new Long(10)}), // fib index
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {new Integer(0)}),
                // method begins here
                // local var i declared
                new Instruction(YARVInstructions.PUTOBJECT, new Object[] {new Long(0)}),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {new Integer(1)}),
                // local var j declared
                new Instruction(YARVInstructions.PUTOBJECT, new Object[] {new Long(1)}),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {new Integer(2)}),
                // local var cur declared
                new Instruction(YARVInstructions.PUTOBJECT, new Object[] {new Long(1)}),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {new Integer(3)}),
                // while begins here, instruction 8
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {new Integer(3)}),
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {new Integer(0)}),
                new Instruction(YARVInstructions.SEND, new Object[] {"<=", new Integer(1), null, new Integer(0)}),
                new Instruction(YARVInstructions.BRANCHUNLESS, new Object[] {new Integer(25)}),
                // local var k declared, k = i
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {new Integer(1)}),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {new Integer(4)}),
                // i = j
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {new Integer(2)}),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {new Integer(1)}),
                // j = k + j
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {new Integer(4)}),
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {new Integer(2)}),
                new Instruction(YARVInstructions.SEND, new Object[] {"+", new Integer(1), null, new Integer(0)}),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {new Integer(2)}),
                // cur = cur + 1
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {new Integer(3)}),
                new Instruction(YARVInstructions.PUTOBJECT, new Object[] {new Long(1)}),
                new Instruction(YARVInstructions.SEND, new Object[] {"+", new Integer(1), null, new Integer(0)}),
                new Instruction(YARVInstructions.SETLOCAL, new Object[] {new Integer(3)}),
                // end while
                new Instruction(YARVInstructions.JUMP, new Object[] {new Integer(8)}),
                // return i, instruction 25
                new Instruction(YARVInstructions.GETLOCAL, new Object[] {new Integer(1)})
        };
        
        IRuby runtime = Ruby.newInstance(System.in, System.out, System.err);
        ThreadContext context = runtime.getCurrentContext();
        
        //context.getFrameScope().addLocalVariables(new String[] {"n", "i", "j", "cur", "k"});
        
        assertEquals("55", ym.exec(context, runtime.getObject(), iseq).toString());
        
        iseq[0] = new Instruction(YARVInstructions.PUTOBJECT, new Object[] {new Long(5000)});
        
        IRubyObject fib5k = ym.exec(context, runtime.getObject(), iseq);
        assertEquals("38789684543883256337019163083259053120821277146462451061605972148955501390440370" +
                "9701082291646221066947929345285888297381348310200895498294036143015691147893836421656" +
                "3944106910214505634133706558656238254656700712525929903854933813928836378347518908762" +
                "9707120333370529231076930085180938498018038478139967488817655546537882916442689129803" +
                "8461377896902150229308247566634622492307188332480328037503913035290330450584270114763" +
                "5242270210934637699104006714174883298422891491273104054328753298044273676822977244987" +
                "7498745556919077038806370468327948113589737399931101062193081490185708153978543791953" +
                "0561751076105307568878376603366735544525884488624161921055345749367589784902798823435" +
                "1023599844663934853256411952221859563060475364645470760330902420806382584929156452876" +
                "2915757591423438091423029174910889841552098544324865940797935713168416928680395453095" +
                "4538869811466508206686289742063932343848846524098874239587380197699382031717420893226" +
                "5468879364002630797780058759129671389634214252579116872755600360311370547754724604639" +
                "987588046985178408674382863125", fib5k.toString());
    }
}
