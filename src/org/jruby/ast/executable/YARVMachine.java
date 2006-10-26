package org.jruby.ast.executable;

import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarNode;
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
            case YARVInstructions.GETCLASSVARIABLE: {
                RubyModule rubyClass = context.getRubyClass();
                String name = (String)bytecodes[ip].op1;
    
                if (rubyClass == null) {
                    stack[++stackTop] = self.getMetaClass().getClassVar(name);
                } else if (!rubyClass.isSingleton()) {
                    stack[++stackTop] = rubyClass.getClassVar(name);
                } else {
                    RubyModule module = (RubyModule) rubyClass.getInstanceVariable("__attached__");
    
                    if (module != null) {
                        stack[++stackTop] = module.getClassVar(name);
                    } else {
                        stack[++stackTop] = context.getRuntime().getNil();
                    }
                }
                break;
            }
            case YARVInstructions.SETCLASSVARIABLE: {
                RubyModule rubyClass = (RubyModule) context.peekCRef().getValue();
    
                if (rubyClass == null) {
                    rubyClass = self.getMetaClass();
                } else if (rubyClass.isSingleton()) {
                    rubyClass = (RubyModule) rubyClass.getInstanceVariable("__attached__");
                }
    
                rubyClass.setClassVar((String)bytecodes[ip].op1, (IRubyObject)stack[stackTop--]);
                break;
            }
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
            case YARVInstructions.TOSTRING:
                // TODO: do not call to_s if it's already a string...
                stack[stackTop] = ((IRubyObject)stack[stackTop]).callMethod("to_s");
                break;
            case YARVInstructions.TOREGEXP:
                break;
            case YARVInstructions.NEWARRAY: {
                int size = ((Integer)bytecodes[ip].op1).intValue();
                RubyArray array = context.getRuntime().newArray();
                for (int i = size - 1; i >= 0; i--) {
                    array.set(i, stack[stackTop--]);
                }
                stack[++stackTop] = array;
                break;
            }
            case YARVInstructions.DUPARRAY:
                break;
            case YARVInstructions.EXPANDARRAY:
                // masgn array to values
                break;
            case YARVInstructions.CONCATARRAY:
                break;
            case YARVInstructions.SPLATARRAY:
                break;
            case YARVInstructions.CHECKINCLUDEARRAY:
                break;
            case YARVInstructions.NEWHASH:
                break;
            case YARVInstructions.NEWRANGE:
                break;
            case YARVInstructions.PUTNOT:
                break;
            case YARVInstructions.POP:
                stackTop--;
                break;
            case YARVInstructions.DUP:
                stack[stackTop + 1] = stack[stackTop];
                stackTop++;
                break;
            case YARVInstructions.DUPN: {
                int size = ((Integer)bytecodes[ip].op1).intValue();
                for (int i = 0; i < size; i++) {
                    stack[stackTop + 1] = stack[stackTop - size];
                    stackTop++;
                }
                break;
            }
            case YARVInstructions.SWAP:
                stack[stackTop + 1] = stack[stackTop];
                stack[stackTop] = stack[stackTop - 1];
                stack[stackTop - 1] = stack[stackTop + 1];
            case YARVInstructions.REPUT:
                break;
            case YARVInstructions.TOPN: {
                int n = ((Integer)bytecodes[ip].op1).intValue();
                Object o = stack[stackTop - n];
                stack[++stackTop] = o;
                break;
            }
            case YARVInstructions.SETN: {
                int n = ((Integer)bytecodes[ip].op1).intValue();
                stack[stackTop - n] = stack[stackTop];
                break;
            }
            case YARVInstructions.EMPTSTACK:
                stackTop = -1;
                break;
            }
            ip++;
        }
        
        return (IRubyObject)stack[stackTop];
    }
}
