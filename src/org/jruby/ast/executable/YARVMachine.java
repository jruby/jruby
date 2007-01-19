package org.jruby.ast.executable;

import java.util.Map;
import java.util.HashMap;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.MetaClass;
import org.jruby.parser.StaticScope;
import org.jruby.parser.LocalStaticScope;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.methods.YARVMethod;
import org.jruby.internal.runtime.methods.WrapperMethod;

public class YARVMachine {
    private static final boolean TAILCALL_OPT = Boolean.getBoolean("jruby.tailcall.enabled");

    public static int instruction(String name) {
        return YARVInstructions.instruction(name);
    }

    public static class InstructionSequence {
        String magic;
        int major;
        int minor;
        int format_type;
        Object misc;
        String name;
        String filename;
        Object[] line;
        String type;

        String[] locals;

        public int args_argc;
        public int args_arg_opts;
        public String[] args_opt_labels;
        public int args_rest;
        public int args_block;

        Object[] exception;

        public Instruction[] body;
    }

    public static class Instruction {
        final int bytecode;
        String s_op0;
        IRubyObject o_op0;
        long l_op0;
        int i_op1;
        InstructionSequence iseq_op;
        Instruction[] ins_op;
        int i_op3;

        int index;

        public Instruction(int bytecode) {
            this.bytecode = bytecode;
        }

        public Instruction(int bytecode, String op) {
            this.bytecode = bytecode;
            this.s_op0 = op;
        }

        public Instruction(int bytecode, String op, InstructionSequence op1) {
            this.bytecode = bytecode;
            this.s_op0 = op;
            this.iseq_op = op1;
        }

        public Instruction(int bytecode, long op) {
            this.bytecode = bytecode;
            this.l_op0 = op;
        }

        public Instruction(int bytecode, IRubyObject op) {
            this.bytecode = bytecode;
            this.o_op0 = op;
        }

        public Instruction(int bytecode, String op, int op1, Instruction[] op2, int op3) {
            this.bytecode = bytecode;
            this.s_op0 = op;
            this.i_op1 = op1;
            this.ins_op = op2;
            this.i_op3 = op3;
        }
    }
    
    public IRubyObject exec(ThreadContext context, IRubyObject self, StaticScope scope, Instruction[] bytecodes) {
        return exec(context,self, new DynamicScope(scope,null),bytecodes);
    }

    public IRubyObject exec(ThreadContext context, IRubyObject self, DynamicScope scope, Instruction[] bytecodes) {
        IRubyObject[] stack = new IRubyObject[50];
        int stackTop = 0;
        stack[stackTop] = context.getRuntime().getNil();
        int ip = 0;
        IRuby runtime = context.getRuntime();
        context.preRootNode(scope);
        IRubyObject recv;
        IRubyObject other;

        yarvloop: while (ip < bytecodes.length) {
            switch (bytecodes[ip].bytecode) {
            case YARVInstructions.NOP:
                break;
            case YARVInstructions.GETLOCAL:
                stack[++stackTop] = context.getCurrentScope().getValues()[(int)bytecodes[ip].l_op0];
                break;
            case YARVInstructions.SETLOCAL:
                context.getCurrentScope().getValues()[(int)bytecodes[ip].l_op0] = stack[stackTop--];
                break;
            case YARVInstructions.GETSPECIAL:
                System.err.println("Not implemented, YARVMachine." + YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.SETSPECIAL:
                System.err.println("Not implemented, YARVMachine." + YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.GETDYNAMIC:
                System.err.println("Not implemented, YARVMachine." + YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.SETDYNAMIC:
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.GETINSTANCEVARIABLE:
                stack[++stackTop] = self.getInstanceVariable(bytecodes[ip].s_op0);
                break;
            case YARVInstructions.SETINSTANCEVARIABLE:
                self.setInstanceVariable(bytecodes[ip].s_op0, stack[stackTop--]);
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
    
                rubyClass.setClassVar(bytecodes[ip].s_op0, stack[stackTop--]);
                break;
            }
            case YARVInstructions.GETCONSTANT:
                stack[++stackTop] = context.getConstant(bytecodes[ip].s_op0);
                break;
            case YARVInstructions.SETCONSTANT:
                RubyModule module = (RubyModule) context.peekCRef().getValue();
                module.setConstant(bytecodes[ip].s_op0,stack[stackTop--]);
                break;
            case YARVInstructions.GETGLOBAL:
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.SETGLOBAL:
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.PUTNIL:
                stack[++stackTop] = context.getRuntime().getNil();
                break;
            case YARVInstructions.PUTSELF:
                stack[++stackTop] = self;
                break;
            case YARVInstructions.PUTUNDEF:
                // ko1 said this is going away
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.PUTOBJECT:
                stack[++stackTop] = bytecodes[ip].o_op0;
                break;
            case YARVInstructions.PUTSTRING:
                stack[++stackTop] = context.getRuntime().newString(bytecodes[ip].s_op0);
                break;
            case YARVInstructions.CONCATSTRINGS: {
                StringBuffer concatter = new StringBuffer();
                for (int i = 0; i < bytecodes[ip].l_op0; i++) {
                    concatter.append(stack[stackTop--].toString());
                }
                stack[++stackTop] = context.getRuntime().newString(concatter.toString());
                break;
            }
            case YARVInstructions.TOSTRING:
                if(!(stack[stackTop] instanceof RubyString)) {
                    stack[stackTop] = (stack[stackTop]).callMethod(context, "to_s");
                }
                break;
            case YARVInstructions.TOREGEXP:
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
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
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.EXPANDARRAY:
                // masgn array to values
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.CONCATARRAY:
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.SPLATARRAY:
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.CHECKINCLUDEARRAY:
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.NEWHASH:
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.NEWRANGE:
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.PUTNOT:
                stack[stackTop+1] = stack[stackTop].isTrue() ? runtime.getFalse() : runtime.getTrue();
                stackTop++;
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
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.TOPN: {
                int n = (int)bytecodes[ip].l_op0;
                other = stack[stackTop - n];
                stack[++stackTop] = other;
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
            case YARVInstructions.DEFINEMETHOD: 
                RubyModule containingClass = context.getRubyClass();
    
                if (containingClass == null) {
                    throw runtime.newTypeError("No class to add method.");
                }

                String mname = bytecodes[ip].iseq_op.name;
                if (containingClass == runtime.getObject() && mname == "initialize") {
                    runtime.getWarnings().warn("redefining Object#initialize may cause infinite loop");
                }
    
                Visibility visibility = context.getCurrentVisibility();
                if (mname == "initialize" || visibility.isModuleFunction() || context.isTopLevel()) {
                    visibility = Visibility.PRIVATE;
                }
                
                if (containingClass.isSingleton()) {
                    IRubyObject attachedObject = ((MetaClass) containingClass).getAttachedObject();
                    
                    if (!attachedObject.singletonMethodsAllowed()) {
                        throw runtime.newTypeError("can't define singleton method \"" + 
                                mname + "\" for " + attachedObject.getType());
                    }
                }

                StaticScope sco = new LocalStaticScope(null);
                sco.setVariables(bytecodes[ip].iseq_op.locals);
                YARVMethod newMethod = new YARVMethod(containingClass, bytecodes[ip].iseq_op, sco, visibility, context.peekCRef());

                containingClass.addMethod(mname, newMethod);
    
                if (context.getCurrentVisibility().isModuleFunction()) {
                    containingClass.getSingletonClass().addMethod(
                            mname,
                            new WrapperMethod(containingClass.getSingletonClass(), newMethod,
                                    Visibility.PUBLIC));
                    containingClass.callMethod(context, "singleton_method_added", runtime.newSymbol(mname));
                }
    
                // 'class << state.self' and 'class << obj' uses defn as opposed to defs
                if (containingClass.isSingleton()) {
                    ((MetaClass) containingClass).getAttachedObject().callMethod(
                            context, "singleton_method_added", runtime.newSymbol(mname));
                } else {
                    containingClass.callMethod(context, "method_added", runtime.newSymbol(mname));
                }
                stack[++stackTop] = runtime.getNil();
                break;
            case YARVInstructions.ALIAS: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.UNDEF: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.DEFINED: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.POSTEXE: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.TRACE: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.DEFINECLASS: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.SEND: {
                context.beginCallArgs();
                String name = bytecodes[ip].s_op0;
                IRubyObject[] args = new IRubyObject[bytecodes[ip].i_op1];

                Instruction[] blockBytecodes = bytecodes[ip].ins_op;
                // TODO: block stuff
                int flags = bytecodes[ip].i_op3;
                CallType callType;

                for (int i = args.length; i > 0; i--) {
                    args[i-1] = stack[stackTop--];
                }

                if ((flags & YARVInstructions.VCALL_FLAG) == 0) {
                    if ((flags & YARVInstructions.FCALL_FLAG) == 0) {
                        recv = stack[stackTop--];
                        callType = CallType.NORMAL;
                    } else {
                        recv = self;
                        callType = CallType.FUNCTIONAL;
                    }
                } else {
                    recv = self;
                    callType = CallType.VARIABLE;
                }
                assert recv.getMetaClass() != null : recv.getClass().getName();

                if(TAILCALL_OPT && (bytecodes[ip+1].bytecode == YARVInstructions.LEAVE || 
                                    (flags & YARVInstructions.TAILCALL_FLAG) == YARVInstructions.TAILCALL_FLAG) &&
                   recv == self && name.equals(context.getFrameLastFunc())) {
                    stackTop = 0;
                    ip = -1;
                    
                    for(int i=0;i<args.length;i++) {
                        context.getCurrentScope().getValues()[i] = args[i];
                    }
                } else {
                    stack[++stackTop] = recv.callMethod(context, name, args, callType);
                }
                break;
            }
            case YARVInstructions.INVOKESUPER: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.INVOKEBLOCK: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.LEAVE:
                break yarvloop;
            case YARVInstructions.FINISH: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.THROW: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
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
            case YARVInstructions.GETINLINECACHE: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.ONCEINLINECACHE: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
                break;
            case YARVInstructions.SETINLINECACHE: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.OPT_CASE_DISPATCH: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.OPT_CHECKENV: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.OPT_PLUS:
                other = stack[stackTop--];
                recv = stack[stackTop--];
                stack[++stackTop] = recv.callMethod(context,"+",other);
                break;
            case YARVInstructions.OPT_MINUS: 
                other = stack[stackTop--];
                recv = stack[stackTop--];
                stack[++stackTop] = recv.callMethod(context,"-",other);
                break;
            case YARVInstructions.OPT_MULT: 
                other = stack[stackTop--];
                recv = stack[stackTop--];
                stack[++stackTop] = recv.callMethod(context,"*",other);
                break;
            case YARVInstructions.OPT_DIV: 
                other = stack[stackTop--];
                recv = stack[stackTop--];
                stack[++stackTop] = recv.callMethod(context,"/",other);
                break;
            case YARVInstructions.OPT_MOD: 
                other = stack[stackTop--];
                recv = stack[stackTop--];
                stack[++stackTop] = recv.callMethod(context,"%",other);
                break;
            case YARVInstructions.OPT_EQ:
                other = stack[stackTop--];
                recv = stack[stackTop--];
                stack[++stackTop] = recv.callMethod(context,"==",other);
                break;
            case YARVInstructions.OPT_LT:
                other = stack[stackTop--];
                recv = stack[stackTop--];
                stack[++stackTop] = recv.callMethod(context,"<",other);
                break;
            case YARVInstructions.OPT_LE: 
                other = stack[stackTop--];
                recv = stack[stackTop--];
                stack[++stackTop] = recv.callMethod(context,"<=",other);
                break;
            case YARVInstructions.OPT_LTLT: 
                other = stack[stackTop--];
                recv = stack[stackTop--];
                stack[++stackTop] = recv.callMethod(context,"<<",other);
                break;
            case YARVInstructions.OPT_AREF: 
                other = stack[stackTop--];
                recv = stack[stackTop--];
                stack[++stackTop] = recv.callMethod(context,"[]",other);
                break;
            case YARVInstructions.OPT_ASET: 
                //YARV will never emit this, for some reason.
                IRubyObject value = stack[stackTop--];
                other = stack[stackTop--];
                recv = stack[stackTop--];
                stack[++stackTop] = recv.callMethod(context,"[]=",new IRubyObject[]{other,value});
                break;
            case YARVInstructions.OPT_LENGTH: 
                recv = stack[stackTop--];
                stack[++stackTop] = recv.callMethod(context,"length");
                break;
            case YARVInstructions.OPT_SUCC: 
                recv = stack[stackTop--];
                stack[++stackTop] = recv.callMethod(context,"succ");
                break;
            case YARVInstructions.OPT_REGEXPMATCH1: 
                stack[stackTop] = bytecodes[ip].o_op0.callMethod(context,"=~",stack[stackTop]);
                break;
            case YARVInstructions.OPT_REGEXPMATCH2:
                other = stack[stackTop--];
                recv = stack[stackTop--];
                stack[++stackTop] = recv.callMethod(context,"=~",other);
                break;
            case YARVInstructions.OPT_CALL_NATIVE_COMPILED: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.BITBLT: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.ANSWER: 
                stack[++stackTop] = runtime.newFixnum(42);
                break;
            case YARVInstructions.GETLOCAL_OP_2: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.GETLOCAL_OP_3: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.GETLOCAL_OP_4: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SETLOCAL_OP_2:
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SETLOCAL_OP_3: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SETLOCAL_OP_4: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.GETDYNAMIC_OP__WC__0: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.GETDYNAMIC_OP_1_0: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.GETDYNAMIC_OP_2_0: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.GETDYNAMIC_OP_3_0: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.GETDYNAMIC_OP_4_0: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SETDYNAMIC_OP__WC__0: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SETDYNAMIC_OP_1_0: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SETDYNAMIC_OP_2_0: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SETDYNAMIC_OP_3_0: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SETDYNAMIC_OP_4_0: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.PUTOBJECT_OP_INT2FIX_0_0_C_: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.PUTOBJECT_OP_INT2FIX_0_1_C_: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.PUTOBJECT_OP_QTRUE: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.PUTOBJECT_OP_QFALSE: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SEND_OP__WC___WC__QFALSE_0__WC_: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SEND_OP__WC__0_QFALSE_0__WC_: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SEND_OP__WC__1_QFALSE_0__WC_: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SEND_OP__WC__2_QFALSE_0__WC_: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SEND_OP__WC__3_QFALSE_0__WC_: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SEND_OP__WC___WC__QFALSE_0X04__WC_: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SEND_OP__WC__0_QFALSE_0X04__WC_: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SEND_OP__WC__1_QFALSE_0X04__WC_: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SEND_OP__WC__2_QFALSE_0X04__WC_: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SEND_OP__WC__3_QFALSE_0X04__WC_: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.SEND_OP__WC__0_QFALSE_0X0C__WC_: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.UNIFIED_PUTOBJECT_PUTOBJECT: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.UNIFIED_PUTOBJECT_PUTSTRING: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.UNIFIED_PUTOBJECT_SETLOCAL: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.UNIFIED_PUTOBJECT_SETDYNAMIC: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.UNIFIED_PUTSTRING_PUTSTRING: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.UNIFIED_PUTSTRING_PUTOBJECT: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.UNIFIED_PUTSTRING_SETLOCAL: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.UNIFIED_PUTSTRING_SETDYNAMIC: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.UNIFIED_DUP_SETLOCAL: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.UNIFIED_GETLOCAL_GETLOCAL: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            case YARVInstructions.UNIFIED_GETLOCAL_PUTOBJECT: 
                System.err.println("Not implemented, YARVMachine." +YARVInstructions.name(bytecodes[ip].bytecode));
break;
            }
            ip++;
        }

        context.postRootNode();
        return stack[stackTop];
    }
}
