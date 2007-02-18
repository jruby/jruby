package org.jruby.ast.executable;

import org.jruby.Ruby;
import org.jruby.ast.executable.YARVMachine.Instruction;
import org.jruby.parser.LocalStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import junit.framework.TestCase;

public class YARVMachineTest extends TestCase {
    public static Instruction[] getSimpleTest(Ruby runtime) {
        return new Instruction[] {
            new Instruction(YARVInstructions.PUTSTRING, "Hello, YARV!"),
            new Instruction(YARVInstructions.DUP),
            new Instruction(YARVInstructions.SETLOCAL, 0),
            new Instruction(YARVInstructions.GETLOCAL, 0),
            new Instruction(YARVInstructions.POP),
            new Instruction(YARVInstructions.SETLOCAL, 1),
            new Instruction(YARVInstructions.PUTOBJECT, runtime.getTrue()),
            new Instruction(YARVInstructions.BRANCHIF, 10),
            new Instruction(YARVInstructions.PUTSTRING, "Wrong String"),
            new Instruction(YARVInstructions.JUMP, 11),
            new Instruction(YARVInstructions.GETLOCAL, 1),
            new Instruction(YARVInstructions.PUTOBJECT, runtime.newFixnum(2)),
            new Instruction(YARVInstructions.SEND, "*", 1, null, 0),
            new Instruction(YARVInstructions.SEND, "to_s", 0, null, YARVInstructions.VCALL_FLAG | YARVInstructions.FCALL_FLAG),
            new Instruction(YARVInstructions.SEND, "+", 1, null, 0)
        };
    };
    
    public static Instruction[] getFib(Ruby runtime, int n){
        return new Instruction[] {
            // local var n declared (argument)
            new Instruction(YARVInstructions.PUTOBJECT, runtime.newFixnum(n)), // fib index
            new Instruction(YARVInstructions.SETLOCAL, 0),
            // method begins here
            // local var i declared
            new Instruction(YARVInstructions.PUTOBJECT, runtime.newFixnum(0)),
            new Instruction(YARVInstructions.SETLOCAL, 1),
            // local var j declared
            new Instruction(YARVInstructions.PUTOBJECT, runtime.newFixnum(1)),
            new Instruction(YARVInstructions.SETLOCAL, 2),
            // local var cur declared
            new Instruction(YARVInstructions.PUTOBJECT, runtime.newFixnum(1)),
            new Instruction(YARVInstructions.SETLOCAL, 3),
            // while begins here, instruction 8
            new Instruction(YARVInstructions.GETLOCAL, 3),
            new Instruction(YARVInstructions.GETLOCAL, 0),
            new Instruction(YARVInstructions.SEND, "<=", 1, null, 0),
            new Instruction(YARVInstructions.BRANCHUNLESS, 25),
            // local var k declared, k = i
            new Instruction(YARVInstructions.GETLOCAL, 1),
            new Instruction(YARVInstructions.SETLOCAL, 4),
            // i = j
            new Instruction(YARVInstructions.GETLOCAL, 2),
            new Instruction(YARVInstructions.SETLOCAL, 1),
            // j = k + j
            new Instruction(YARVInstructions.GETLOCAL, 4),
            new Instruction(YARVInstructions.GETLOCAL, 2),
            new Instruction(YARVInstructions.SEND, "+", 1, null, 0),
            new Instruction(YARVInstructions.SETLOCAL, 2),
            // cur = cur + 1
            new Instruction(YARVInstructions.GETLOCAL, 3),
            new Instruction(YARVInstructions.PUTOBJECT, runtime.newFixnum(1)),
            new Instruction(YARVInstructions.SEND, "+", 1, null, 0),
            new Instruction(YARVInstructions.SETLOCAL, 3),
            // end while
            new Instruction(YARVInstructions.JUMP, 8),
            // return i, instruction 25
            new Instruction(YARVInstructions.GETLOCAL, 1)
        };
    };

    public void testSimpleExecution() {
        YARVMachine ym = new YARVMachine();
        
        Ruby runtime = Ruby.newInstance(System.in, System.out, System.err);
        ThreadContext context = runtime.getCurrentContext();
        
        StaticScope scope = new LocalStaticScope(null);
        scope.setVariables(new String[] { "zero", "one" });
        assertEquals("Hello, YARV!Hello, YARV!Object", ym.exec(context, runtime.getObject(), scope, getSimpleTest(runtime)).toString());
    }
    
    public void testIterativeFib() {
        YARVMachine ym = new YARVMachine();
        
        Ruby runtime = Ruby.newInstance(System.in, System.out, System.err);
        ThreadContext context = runtime.getCurrentContext();
        
        StaticScope scope = new LocalStaticScope(null);
        scope.setVariables(new String[] {"n", "i", "j", "cur", "k"});
        
        assertEquals("55", ym.exec(context, runtime.getObject(), scope, getFib(runtime,10)).toString());
        
        IRubyObject fib5k = ym.exec(context, runtime.getObject(), scope, getFib(runtime,5000));
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
    
    public static void main(String[] args) {
        new YARVMachineTest().testIterativeFib();
    }
}
