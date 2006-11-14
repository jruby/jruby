package org.jruby.ast.executable;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class YARVMachine {
    public static class Instruction {
        final int bytecode;
        final Object[] operands;
        public Instruction(int bytecode, Object[] operands) {
            this.bytecode = bytecode;
            this.operands = operands;
        }
    }
    
    public IRubyObject exec(ThreadContext context, IRubyObject self, Instruction[] bytecodes) {
        IRubyObject[] stack = new IRubyObject[50];
        int stackTop = 0;
        stack[stackTop] = context.getRuntime().getNil();
        int ip = 0;
        IRuby runtime = context.getRuntime();
        
        yarvloop: while (ip < bytecodes.length) {
            switch (bytecodes[ip].bytecode) {
            case YARVInstructions.NOP:
                break;
            case YARVInstructions.GETLOCAL:
                // ENEBO: Not sure if this is correct since a local can have a depth (now that 
                // block and local vars are in same list).
                stack[++stackTop] = context.getCurrentScope().getValue(((Integer)bytecodes[ip].operands[0]).intValue(), 0);
                break;
            case YARVInstructions.SETLOCAL:
                // ENEBO: Not sure if this is correct since a local can have a depth
                context.getCurrentScope().setValue(((Integer)bytecodes[ip].operands[0]).intValue(), (IRubyObject)stack[stackTop--], 0);
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
                stack[++stackTop] = self.getInstanceVariable((String)bytecodes[ip].operands[0]);
                break;
            case YARVInstructions.SETINSTANCEVARIABLE:
                self.setInstanceVariable((String)bytecodes[ip].operands[0], (IRubyObject)stack[stackTop--]);
                break;
            case YARVInstructions.GETCLASSVARIABLE: {
                RubyModule rubyClass = context.getRubyClass();
                String name = (String)bytecodes[ip].operands[0];
    
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
    
                rubyClass.setClassVar((String)bytecodes[ip].operands[0], (IRubyObject)stack[stackTop--]);
                break;
            }
            case YARVInstructions.GETCONSTANT:
                stack[stackTop] = ((RubyModule)stack[stackTop]).getConstant((String)bytecodes[ip].operands[0]);
                break;
            case YARVInstructions.SETCONSTANT:
                ((RubyModule)stack[stackTop--]).setConstant((String)bytecodes[ip].operands[0], (IRubyObject)stack[stackTop--]);
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
                // TODO: needs to avoid using ifs
                if (bytecodes[ip].operands[0] == Boolean.TRUE) {
                    stack[++stackTop] = runtime.getTrue();
                } else if (bytecodes[ip].operands[0] == Boolean.FALSE) {
                    stack[++stackTop] = runtime.getFalse();
                } else if (bytecodes[ip].operands[0] instanceof Long) {
                    stack[++stackTop] = runtime.newFixnum(((Long)bytecodes[ip].operands[0]).longValue());
                }
                break;
            case YARVInstructions.PUTSTRING:
                stack[++stackTop] = context.getRuntime().newString((String)bytecodes[ip].operands[0]);
                break;
            case YARVInstructions.CONCATSTRINGS: {
                StringBuffer concatter = new StringBuffer();
                for (int i = 0; i < ((Integer)bytecodes[ip].operands[0]).intValue(); i++) {
                    concatter.append(((RubyString)stack[stackTop--]).toString());
                }
                stack[++stackTop] = context.getRuntime().newString(concatter.toString());
                break;
            }
            case YARVInstructions.TOSTRING:
                // TODO: do not call to_s if it's already a string...
                stack[stackTop] = ((IRubyObject)stack[stackTop]).callMethod(context, "to_s");
                break;
            case YARVInstructions.TOREGEXP:
                break;
            case YARVInstructions.NEWARRAY: {
                int size = ((Integer)bytecodes[ip].operands[0]).intValue();
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
                int size = ((Integer)bytecodes[ip].operands[0]).intValue();
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
                int n = ((Integer)bytecodes[ip].operands[0]).intValue();
                IRubyObject o = stack[stackTop - n];
                stack[++stackTop] = o;
                break;
            }
            case YARVInstructions.SETN: {
                int n = ((Integer)bytecodes[ip].operands[0]).intValue();
                stack[stackTop - n] = stack[stackTop];
                break;
            }
            case YARVInstructions.EMPTSTACK:
                stackTop = 0;
                break;
            case YARVInstructions.DEFINEMETHOD: break;
            case YARVInstructions.ALIAS: break;
            case YARVInstructions.UNDEF: break;
            case YARVInstructions.DEFINED: break;
            case YARVInstructions.POSTEXE: break;
            case YARVInstructions.TRACE: break;
            case YARVInstructions.DEFINECLASS: break;
            case YARVInstructions.SEND: {
                context.beginCallArgs();
                String name = (String)bytecodes[ip].operands[0];
                IRubyObject receiver = null;
                IRubyObject[] args = new IRubyObject[((Integer)bytecodes[ip].operands[1]).intValue()];
                Instruction[] blockBytecodes = (Instruction[])bytecodes[ip].operands[2];
                // TODO: block stuff
                int flags = ((Integer)bytecodes[ip].operands[3]).intValue();
                CallType callType;
                
                if ((flags & YARVInstructions.VCALL_FLAG) == 0) {
                    for (int i = args.length - 1; i >= 0; i--) {
                        args[i] = stack[stackTop--];
                    }
                    
                    if ((flags & YARVInstructions.FCALL_FLAG) == 0) {
                        receiver = stack[stackTop--];
                        callType = CallType.NORMAL;
                    } else {
                        receiver = self;
                        callType = CallType.FUNCTIONAL;
                    }
                } else {
                    receiver = self;
                    callType = CallType.VARIABLE;
                }

                assert receiver.getMetaClass() != null : receiver.getClass().getName();
                
                stack[++stackTop] = receiver.callMethod(context, name, args, callType);
                break;
            }
            case YARVInstructions.INVOKESUPER: break;
            case YARVInstructions.INVOKEBLOCK: break;
            case YARVInstructions.LEAVE: break;
            case YARVInstructions.FINISH: break;
            case YARVInstructions.THROW: break;
            case YARVInstructions.JUMP:
                ip = ((Integer)bytecodes[ip].operands[0]).intValue();
                continue yarvloop;
            case YARVInstructions.BRANCHIF:
                if (stack[stackTop--].isTrue()) {
                    ip = ((Integer)bytecodes[ip].operands[0]).intValue();
                } else {
                    ip++;
                }
                continue yarvloop;
            case YARVInstructions.BRANCHUNLESS:
                if (!stack[stackTop--].isTrue()) {
                    ip = ((Integer)bytecodes[ip].operands[0]).intValue();
                } else {
                    ip++;
                }
                continue yarvloop;
            case YARVInstructions.GETINLINECACHE: break;
            case YARVInstructions.ONCEINLINECACHE: break;
            case YARVInstructions.SETINLINECACHE: break;
            case YARVInstructions.OPT_CASE_DISPATCH: break;
            case YARVInstructions.OPT_CHECKENV: break;
            case YARVInstructions.OPT_PLUS: break;
            case YARVInstructions.OPT_MINUS: break;
            case YARVInstructions.OPT_MULT: break;
            case YARVInstructions.OPT_DIV: break;
            case YARVInstructions.OPT_MOD: break;
            case YARVInstructions.OPT_EQ: break;
            case YARVInstructions.OPT_LT: break;
            case YARVInstructions.OPT_LE: break;
            case YARVInstructions.OPT_LTLT: break;
            case YARVInstructions.OPT_AREF: break;
            case YARVInstructions.OPT_ASET: break;
            case YARVInstructions.OPT_LENGTH: break;
            case YARVInstructions.OPT_SUCC: break;
            case YARVInstructions.OPT_REGEXPMATCH1: break;
            case YARVInstructions.OPT_REGEXPMATCH2: break;
            case YARVInstructions.OPT_CALL_NATIVE_COMPILED: break;
            case YARVInstructions.BITBLT: break;
            case YARVInstructions.ANSWER: break;
            case YARVInstructions.GETLOCAL_OP_2: break;
            case YARVInstructions.GETLOCAL_OP_3: break;
            case YARVInstructions.GETLOCAL_OP_4: break;
            case YARVInstructions.SETLOCAL_OP_2: break;
            case YARVInstructions.SETLOCAL_OP_3: break;
            case YARVInstructions.SETLOCAL_OP_4: break;
            case YARVInstructions.GETDYNAMIC_OP__WC__0: break;
            case YARVInstructions.GETDYNAMIC_OP_1_0: break;
            case YARVInstructions.GETDYNAMIC_OP_2_0: break;
            case YARVInstructions.GETDYNAMIC_OP_3_0: break;
            case YARVInstructions.GETDYNAMIC_OP_4_0: break;
            case YARVInstructions.SETDYNAMIC_OP__WC__0: break;
            case YARVInstructions.SETDYNAMIC_OP_1_0: break;
            case YARVInstructions.SETDYNAMIC_OP_2_0: break;
            case YARVInstructions.SETDYNAMIC_OP_3_0: break;
            case YARVInstructions.SETDYNAMIC_OP_4_0: break;
            case YARVInstructions.PUTOBJECT_OP_INT2FIX_0_0_C_: break;
            case YARVInstructions.PUTOBJECT_OP_INT2FIX_0_1_C_: break;
            case YARVInstructions.PUTOBJECT_OP_QTRUE: break;
            case YARVInstructions.PUTOBJECT_OP_QFALSE: break;
            case YARVInstructions.SEND_OP__WC___WC__QFALSE_0__WC_: break;
            case YARVInstructions.SEND_OP__WC__0_QFALSE_0__WC_: break;
            case YARVInstructions.SEND_OP__WC__1_QFALSE_0__WC_: break;
            case YARVInstructions.SEND_OP__WC__2_QFALSE_0__WC_: break;
            case YARVInstructions.SEND_OP__WC__3_QFALSE_0__WC_: break;
            case YARVInstructions.SEND_OP__WC___WC__QFALSE_0x04__WC_: break;
            case YARVInstructions.SEND_OP__WC__0_QFALSE_0x04__WC_: break;
            case YARVInstructions.SEND_OP__WC__1_QFALSE_0x04__WC_: break;
            case YARVInstructions.SEND_OP__WC__2_QFALSE_0x04__WC_: break;
            case YARVInstructions.SEND_OP__WC__3_QFALSE_0x04__WC_: break;
            case YARVInstructions.SEND_OP__WC__0_QFALSE_0x0c__WC_: break;
            case YARVInstructions.UNIFIED_PUTOBJECT_PUTOBJECT: break;
            case YARVInstructions.UNIFIED_PUTOBJECT_PUTSTRING: break;
            case YARVInstructions.UNIFIED_PUTOBJECT_SETLOCAL: break;
            case YARVInstructions.UNIFIED_PUTOBJECT_SETDYNAMIC: break;
            case YARVInstructions.UNIFIED_PUTSTRING_PUTSTRING: break;
            case YARVInstructions.UNIFIED_PUTSTRING_PUTOBJECT: break;
            case YARVInstructions.UNIFIED_PUTSTRING_SETLOCAL: break;
            case YARVInstructions.UNIFIED_PUTSTRING_SETDYNAMIC: break;
            case YARVInstructions.UNIFIED_DUP_SETLOCAL: break;
            case YARVInstructions.UNIFIED_GETLOCAL_GETLOCAL: break;
            case YARVInstructions.UNIFIED_GETLOCAL_PUTOBJECT: break;
            }
            ip++;
        }
        
        return stack[stackTop];
    }
}
