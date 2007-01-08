package org.jruby.ast.executable;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class YARVMachine {
    public static class Instruction {
        final int bytecode;
        String s_op0;
        boolean b_op0;
        boolean has_b = false;
        long l_op0;
        int i_op1;
        Instruction[] ins_op2;
        int i_op3;

        public Instruction(int bytecode) {
            this.bytecode = bytecode;
        }

        public Instruction(int bytecode, String op) {
            this.bytecode = bytecode;
            this.s_op0 = op;
        }

        public Instruction(int bytecode, long op) {
            this.bytecode = bytecode;
            this.l_op0 = op;
        }

        public Instruction(int bytecode, boolean op) {
            this.bytecode = bytecode;
            this.b_op0 = op;
            this.has_b = true;
        }

        public Instruction(int bytecode, String op, int op1, Instruction[] op2, int op3) {
            this.bytecode = bytecode;
            this.s_op0 = op;
            this.i_op1 = op1;
            this.ins_op2 = op2;
            this.i_op3 = op3;
        }
    }
    
    public IRubyObject exec(ThreadContext context, IRubyObject self, StaticScope scope, Instruction[] bytecodes) {
        IRubyObject[] stack = new IRubyObject[50];
        int stackTop = 0;
        stack[stackTop] = context.getRuntime().getNil();
        int ip = 0;
        IRuby runtime = context.getRuntime();
        context.preRootNode(new DynamicScope(scope, null));
        
        yarvloop: while (ip < bytecodes.length) {
            switch (bytecodes[ip].bytecode) {
            case YARVInstructions.NOP:
                break;
            case YARVInstructions.GETLOCAL:
                // ENEBO: Not sure if this is correct since a local can have a depth (now that 
                // block and local vars are in same list).
                stack[++stackTop] = context.getCurrentScope().getValue((int)bytecodes[ip].l_op0, 0);
                break;
            case YARVInstructions.SETLOCAL:
                // ENEBO: Not sure if this is correct since a local can have a depth
                context.getCurrentScope().setValue((int)bytecodes[ip].l_op0, (IRubyObject)stack[stackTop--], 0);
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
                stack[++stackTop] = self.getInstanceVariable(bytecodes[ip].s_op0);
                break;
            case YARVInstructions.SETINSTANCEVARIABLE:
                self.setInstanceVariable(bytecodes[ip].s_op0, (IRubyObject)stack[stackTop--]);
                break;
            case YARVInstructions.GETCLASSVARIABLE: {
                RubyModule rubyClass = context.getRubyClass();
                String name = bytecodes[ip].s_op0;
    
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
    
                rubyClass.setClassVar(bytecodes[ip].s_op0, (IRubyObject)stack[stackTop--]);
                break;
            }
            case YARVInstructions.GETCONSTANT:
                stack[stackTop] = ((RubyModule)stack[stackTop]).getConstant(bytecodes[ip].s_op0);
                break;
            case YARVInstructions.SETCONSTANT:
                ((RubyModule)stack[stackTop--]).setConstant(bytecodes[ip].s_op0, (IRubyObject)stack[stackTop--]);
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
                if(bytecodes[ip].has_b) {
                    stack[++stackTop] = bytecodes[ip].b_op0 ? runtime.getTrue() : runtime.getFalse();
                } else {
                    stack[++stackTop] = runtime.newFixnum(bytecodes[ip].l_op0);
                }
                break;
            case YARVInstructions.PUTSTRING:
                stack[++stackTop] = context.getRuntime().newString(bytecodes[ip].s_op0);
                break;
            case YARVInstructions.CONCATSTRINGS: {
                StringBuffer concatter = new StringBuffer();
                for (int i = 0; i < bytecodes[ip].l_op0; i++) {
                    concatter.append(((RubyString)stack[stackTop--]).toString());
                }
                stack[++stackTop] = context.getRuntime().newString(concatter.toString());
                break;
            }
            case YARVInstructions.TOSTRING:
                if(!(stack[stackTop] instanceof RubyString)) {
                    stack[stackTop] = ((IRubyObject)stack[stackTop]).callMethod(context, "to_s");
                }
                break;
            case YARVInstructions.TOREGEXP:
                break;
            case YARVInstructions.NEWARRAY: {
                int size = (int)bytecodes[ip].l_op0;
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
                int size = (int)bytecodes[ip].l_op0;
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
                int n = (int)bytecodes[ip].l_op0;
                IRubyObject o = stack[stackTop - n];
                stack[++stackTop] = o;
                break;
            }
            case YARVInstructions.SETN: {
                int n = (int)bytecodes[ip].l_op0;
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
                String name = bytecodes[ip].s_op0;
                IRubyObject receiver = null;
                IRubyObject[] args = new IRubyObject[bytecodes[ip].i_op1];

                Instruction[] blockBytecodes = bytecodes[ip].ins_op2;
                // TODO: block stuff
                int flags = bytecodes[ip].i_op3;
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
                ip = (int)bytecodes[ip].l_op0;
                continue yarvloop;
            case YARVInstructions.BRANCHIF:
                if (stack[stackTop--].isTrue()) {
                    ip = (int)bytecodes[ip].l_op0;
                } else {
                    ip++;
                }
                continue yarvloop;
            case YARVInstructions.BRANCHUNLESS:
                if (!stack[stackTop--].isTrue()) {
                    ip = (int)bytecodes[ip].l_op0;
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
