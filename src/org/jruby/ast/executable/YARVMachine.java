package org.jruby.ast.executable;

import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class YARVMachine {
    public class Instruction {
        int bytecode;
        Object op1;
        Object op2;
        Object op3;
        Object op4;
        Object op5;
    }
    
    public IRubyObject exec(ThreadContext context, IRubyObject self, Instruction[] bytecodes) {
        Object[] stack = new Object[50];
        int stackTop = 0;
        stack[stackTop] = context.getRuntime().getNil();
        int ip = 0;
        
        while (ip < bytecodes.length) {
            switch (bytecodes[ip].bytecode) {
            case YARVInstructions.NOP:
                break;
            case YARVInstructions.GETLOCAL:
                stack[++stackTop] = context.getFrameScope().getValue(((Integer)bytecodes[ip].op1).intValue());
                break;
            case YARVInstructions.SETLOCAL:
                context.getFrameScope().setValue(((Integer)bytecodes[ip].op1).intValue(), (IRubyObject)stack[stackTop--]);
                break;
            case YARVInstructions.GETSPECIAL:
                break;
            case YARVInstructions.SETSPECIAL:
                break;
            case YARVInstructions.GETDYNAMIC:
                break;
            case YARVInstructions.SETDYNAMIC:
                break;
            case YARVInstructions.GETINSTANCEVARIABLE:
                stack[++stackTop] = self.getInstanceVariable((String)bytecodes[ip].op1);
                break;
            case YARVInstructions.SETINSTANCEVARIABLE:
                self.setInstanceVariable((String)bytecodes[ip].op1, (IRubyObject)stack[stackTop--]);
                break;
            case YARVInstructions.GETCLASSVARIABLE:
                break;
            case YARVInstructions.SETCLASSVARIABLE:
                break;
            case YARVInstructions.GETCONSTANT:
                stack[stackTop] = ((RubyModule)stack[stackTop]).getConstant((String)bytecodes[ip].op1);
                break;
            case YARVInstructions.SETCONSTANT:
                ((RubyModule)stack[stackTop--]).setConstant((String)bytecodes[ip].op1, (IRubyObject)stack[stackTop--]);
                break;
            case YARVInstructions.GETGLOBAL:
                break;
            case YARVInstructions.SETGLOBAL:
                break;
            case YARVInstructions.PUTNIL:
                stack[++stackTop] = context.getRuntime().getNil();
                break;
            case YARVInstructions.PUTSELF:
                stack[++stackTop] = self;
                break;
            case YARVInstructions.PUTUNDEF:
                // ko1 said this is going away
                break;
            case YARVInstructions.PUTOBJECT:
                // I think this is for immediates...must check YARV source or ask ko1.
                break;
            case YARVInstructions.PUTSTRING:
                stack[++stackTop] = context.getRuntime().newString((String)bytecodes[ip].op1);
                break;
            case YARVInstructions.CONCATSTRINGS: {
                StringBuffer concatter = new StringBuffer();
                for (int i = 0; i < ((Integer)bytecodes[ip].op1).intValue(); i++) {
                    concatter.append(((RubyString)stack[stackTop--]).toString());
                }
                stack[++stackTop] = context.getRuntime().newString(concatter.toString());
                break;
            }
            }
            ip++;
        }
        
        return (IRubyObject)stack[stackTop];
    }
}
