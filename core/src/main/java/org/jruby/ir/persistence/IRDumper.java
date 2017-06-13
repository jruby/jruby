/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.AsString;
import org.jruby.ir.operands.Bignum;
import org.jruby.ir.operands.ClosureLocalVariable;
import org.jruby.ir.operands.Complex;
import org.jruby.ir.operands.CurrentScope;
import org.jruby.ir.operands.DynamicSymbol;
import org.jruby.ir.operands.Filename;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.FrozenString;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Hash;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Nil;
import org.jruby.ir.operands.NthRef;
import org.jruby.ir.operands.NullBlock;
import org.jruby.ir.operands.ObjectClass;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Rational;
import org.jruby.ir.operands.Regexp;
import org.jruby.ir.operands.SValue;
import org.jruby.ir.operands.ScopeModule;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.StandardError;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.SymbolProc;
import org.jruby.ir.operands.TemporaryBooleanVariable;
import org.jruby.ir.operands.TemporaryFixnumVariable;
import org.jruby.ir.operands.TemporaryFloatVariable;
import org.jruby.ir.operands.TemporaryLocalVariable;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.UnboxedBoolean;
import org.jruby.ir.operands.UnboxedFixnum;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.UnexecutableNil;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.runtime.Signature;
import org.jruby.util.KeyValuePair;
import org.jruby.util.cli.Options;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IRDumper extends IRVisitor {
    private final PrintStream stream;
    private final boolean color;

    public IRDumper(PrintStream ps, boolean color) {
        this.stream = ps;
        this.color = color;
    }

    public static ByteArrayOutputStream printIR(IRScope scope, boolean full) {
        return printIR(scope, full, false);
    }

    public static ByteArrayOutputStream printIR(IRScope scope, boolean full, boolean recurse) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        IRDumper dumper = new IRDumper(ps, Options.IR_PRINT_COLOR.load());
        dumper.visit(scope, full, recurse);
        return baos;
    }

    public void visit(IRScope scope, boolean full, boolean recurse) {
        println("begin " + scope.getScopeType().name() + "<" + scope.getName() + ">");

        InterpreterContext ic = full ? scope.getFullInterpreterContext() : scope.getInterpreterContext();

        println("flags: " + ic.getFlags());

        if (ic.getStaticScope().getSignature() == null) {
            println(Signature.NO_ARGUMENTS);
        } else {
            println(ic.getStaticScope().getSignature());
        }

        Map<String, LocalVariable> localVariables = ic.getScope().getLocalVariables();

        if (localVariables != null && !localVariables.isEmpty()) {
            println("declared variables");

            for (Map.Entry<String, LocalVariable> entry : localVariables.entrySet()) {
                println(ansiStr(VARIABLE_COLOR, "  " + entry.getValue().toString()));
            }
        }

        Set<LocalVariable> usedVariables = ic.getScope().getUsedLocalVariables();

        if (usedVariables != null && !usedVariables.isEmpty()) {
            println("used variables");

            for (LocalVariable var : usedVariables) {
                println(ansiStr(VARIABLE_COLOR, "  " + var.toString()));
            }
        }

        Instr[] instrs = ic.getInstructions();

        // find longest variable name
        int longest = 0;
        int largestBlock = 0;

        if (instrs != null) {
            largestBlock = instrs.length;
            for (Instr i : instrs) {
                if (i instanceof ResultInstr) {
                    longest = getLongestVariable(longest, (ResultInstr) i);
                }
            }
        } else {
            BasicBlock[] bbs = ((FullInterpreterContext)ic).getLinearizedBBList();

            for (BasicBlock bb : bbs) {
                List<Instr> instrList = bb.getInstrs();
                largestBlock = Math.max(largestBlock, instrList.size());
                for (Instr i : instrList) {
                    if (i instanceof ResultInstr) {
                        longest = getLongestVariable(longest, (ResultInstr) i);
                    }
                }
            }
        }

        int instrLog = (int)Math.log10(largestBlock) + 1;

        String varFormat = ansiStr(VARIABLE_COLOR, "%" + longest + "s") + " := ";
        String varSpaces = spaces(longest + " := ".length());
        String ipcFormat = "  %0" + instrLog + "d: ";

        if (instrs != null) {
            for (int i = 0; i < instrs.length; i++) {
                formatInstr(instrs[i], varFormat, varSpaces, ipcFormat, instrs[i], i);
            }
        } else {
            BasicBlock[] bbs = ((FullInterpreterContext)ic).getLinearizedBBList();

            for (BasicBlock bb : bbs) {
                printAnsi(BLOCK_COLOR, "\nblock #" + bb.getID());

                Iterable<BasicBlock> outs;
                if (scope.getCFG() != null &&
                        (outs = scope.getCFG().getOutgoingDestinations(bb)) != null &&
                        outs.iterator().hasNext()) {

                    printAnsi(BLOCK_COLOR, " (out: ");

                    boolean first = true;
                    for (BasicBlock out : outs) {
                        if (!first) printAnsi(BLOCK_COLOR, ",");
                        first = false;
                        printAnsi(BLOCK_COLOR, "" + out.getID());
                    }

                    printAnsi(BLOCK_COLOR, ")");
                }

                printAnsi(BLOCK_COLOR, ": " + bb.getLabel() + "\n");

                List<Instr> instrList = bb.getInstrs();

                for (int i = 0; i < instrList.size(); i++) {
                    formatInstr(instrList.get(i), varFormat, varSpaces, ipcFormat, instrList.get(i), i);
                }
            }
        }

        if (recurse && !scope.getClosures().isEmpty()) {
            println();

            for (IRClosure closure : scope.getClosures()) {
                if (closure == scope) continue;

                visit(closure, full, true);
            }
        }
    }

    public void formatInstr(Instr instr1, String varFormat, String varSpaces, String ipcFormat, Instr instr2, int i) {
        Instr instr = instr2;

        printf(ipcFormat, i);

        if (instr instanceof ResultInstr) {
            Variable result = ((ResultInstr) instr).getResult();
            String sigilName = (result instanceof LocalVariable) ? "*" + result.getName() : result.getName();

            printf(varFormat, sigilName);
        } else {
            print(varSpaces);
        }

        visit(instr1);

        println();
    }

    public int getLongestVariable(int longest, ResultInstr i) {
        Variable result = i.getResult();

        longest = Math.max(longest, result.getName().length() + ((result instanceof LocalVariable) ? 1 : 0));
        return longest;
    }

    @Override
    public void visit(Instr instr) {
        printAnsi(INSTR_COLOR, instr.getOperation().toString().toLowerCase());

        boolean comma = false;

        for (Operand o : instr.getOperands()) {
            if (!comma) printAnsi(INSTR_COLOR, "(");
            if (comma) print(", ");
            comma = true;

            visit(o);
        }

        for (Field f : instr.dumpableFields()) {
            if (!comma) printAnsi(INSTR_COLOR, "(");
            if (comma) print(", ");
            comma = true;

            f.setAccessible(true);

            printAnsi(FIELD_COLOR, f.getName() + ": ");

            print(get(f, instr));
        }

        if (comma) printAnsi(INSTR_COLOR, ")");
    }

    @Override
    public void visit(Operand operand) {
        // Handle variables separately
        if (operand instanceof LocalVariable) {
            printAnsiOp(VARIABLE_COLOR, "*", operand);

        } else if (operand instanceof TemporaryVariable) {
            printAnsiOp(VARIABLE_COLOR, operand);

        } else {
            // Other operand forms need type identification
            printAnsi(OPERAND_COLOR, operand.getOperandType().shortName() + "<");
            operand.visit(this);
            printAnsi(OPERAND_COLOR, ">");
        }
    }

    public void Array(Array array) {
        final boolean[] comma = {false};
        for (Operand o : Arrays.asList(array.getElts())) {
            if (comma[0]) print(", ");
            comma[0] = true;

            visit(o);
        }
    }
    public void AsString(AsString asstring) { visit(asstring.getSource()); }
    public void Bignum(Bignum bignum) { print(bignum.value); }
    public void Boolean(org.jruby.ir.operands.Boolean bool) { print(bool.isTrue() ? "t" : "f"); }
    public void UnboxedBoolean(UnboxedBoolean bool) { print(bool.isTrue() ? "t" : "f"); }
    public void ClosureLocalVariable(ClosureLocalVariable closurelocalvariable) { LocalVariable(closurelocalvariable); }
    public void Complex(Complex complex) { visit(complex.getNumber()); }
    public void CurrentScope(CurrentScope currentscope) { print(currentscope.getScopeNestingDepth()); }
    public void DynamicSymbol(DynamicSymbol dynamicsymbol) { print(dynamicsymbol.getSymbolName()); }
    public void Filename(Filename filename) { }
    public void Fixnum(Fixnum fixnum) { print(fixnum.getValue()); }
    public void FrozenString(FrozenString frozen) { print(frozen.getByteList()); }
    public void UnboxedFixnum(UnboxedFixnum fixnum) { print(fixnum.getValue()); }
    public void Float(org.jruby.ir.operands.Float flote) { print(flote.getValue()); }
    public void UnboxedFloat(org.jruby.ir.operands.UnboxedFloat flote) { print(flote.getValue()); }
    public void GlobalVariable(GlobalVariable globalvariable) { print(globalvariable.getName()); }
    public void Hash(Hash hash) {
        List<KeyValuePair<Operand, Operand>> pairs = hash.getPairs();
        boolean comma = false;
        for (KeyValuePair<Operand, Operand> pair: pairs) {
            if (comma == true) print(',');
            comma = true;
            visit(pair.getKey());
            print("=>");
            visit(pair.getValue());
        }
    }
    public void IRException(IRException irexception) { print(irexception.getType()); }
    public void Label(Label label) { print(label.toString()); }
    public void LocalVariable(LocalVariable localvariable) { print(localvariable.getName()); }
    public void Nil(Nil nil) { }
    public void NthRef(NthRef nthref) { print(nthref.getName()); }
    public void NullBlock(NullBlock nullblock) { }
    public void ObjectClass(ObjectClass objectclass) { }
    public void Rational(Rational rational) { print(rational.getNumerator() + "/" + rational.getDenominator()); }
    public void Regexp(Regexp regexp) { print(regexp.getSource()); }
    public void ScopeModule(ScopeModule scopemodule) { print(scopemodule.getScopeModuleDepth()); }
    public void Self(Self self) { print("%self"); }
    public void Splat(Splat splat) { visit(splat.getArray()); }
    public void StandardError(StandardError standarderror) {  }
    public void StringLiteral(StringLiteral stringliteral) { print(stringliteral.getByteList()); }
    public void SValue(SValue svalue) { visit(svalue.getArray()); }
    public void Symbol(Symbol symbol) { print(symbol.getBytes()); }
    public void SymbolProc(SymbolProc symbolproc) { print(symbolproc.getName()); }
    public void TemporaryVariable(TemporaryVariable temporaryvariable) { print(temporaryvariable.getName()); }
    public void TemporaryLocalVariable(TemporaryLocalVariable temporarylocalvariable) { TemporaryVariable(temporarylocalvariable); }
    public void TemporaryFloatVariable(TemporaryFloatVariable temporaryfloatvariable) { TemporaryVariable(temporaryfloatvariable); }
    public void TemporaryFixnumVariable(TemporaryFixnumVariable temporaryfixnumvariable) { TemporaryVariable(temporaryfixnumvariable); }
    public void TemporaryBooleanVariable(TemporaryBooleanVariable temporarybooleanvariable) { TemporaryVariable(temporarybooleanvariable); }
    public void UndefinedValue(UndefinedValue undefinedvalue) {  }
    public void UnexecutableNil(UnexecutableNil unexecutablenil) {  }
    public void WrappedIRClosure(WrappedIRClosure wrappedirclosure) { print(wrappedirclosure.getClosure().getName()); }

    private static Object get(Field f, Instr i) {
        try {
            return f.get(i);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static final String SPACES = "                                                                             " +
            "                                                                                                          ";

    private static final String spaces(int size) {
        return SPACES.substring(0, size);
    }

    private String ansiStr(String c, String mid) {
        return color ? c + mid + CLEAR_COLOR : mid;
    }

    private void printAnsi(String c, String mid) {
        print(ansiStr(c, mid));
    }

    private void printAnsiOp(String c, Operand op) {
        if (color) print(c);
        op.visit(this);
        if (color) print(CLEAR_COLOR);
    }

    private void printAnsiOp(String c, String pre, Operand op) {
        if (color) print(c);
        print(pre);
        op.visit(this);
        if (color) print(CLEAR_COLOR);
    }

    private void print(Object obj) {
        if (obj.getClass().isArray()) {
            if (obj.getClass().getComponentType().isPrimitive()) {
                switch (obj.getClass().getName().charAt(0)) {
                    case 'B': stream.print(Arrays.toString((boolean[]) obj)); break;
                    case 'S': stream.print(Arrays.toString((short[]) obj)); break;
                    case 'C': stream.print(Arrays.toString((char[]) obj)); break;
                    case 'I': stream.print(Arrays.toString((int[]) obj)); break;
                    case 'J': stream.print(Arrays.toString((long[]) obj)); break;
                    case 'F': stream.print(Arrays.toString((float[]) obj)); break;
                    case 'D': stream.print(Arrays.toString((double[]) obj)); break;
                    case 'Z': stream.print(Arrays.toString((boolean[]) obj)); break;
                }
            } else {
                stream.print(Arrays.toString((Object[]) obj));
            }
        } else {
            stream.print(obj);
        }
    }

    private void println(Object... objs) {
        for (Object obj : objs) print(obj);
        stream.println();
    }

    private void printf(String format, Object... objs) {
        stream.printf(format, objs);
    }

    private static final String INSTR_COLOR = "\033[1;36m";
    private static final String OPERAND_COLOR = "\033[1;33m";
    private static final String VARIABLE_COLOR = "\033[1;32m";
    private static final String FIELD_COLOR = "\033[1;34m";
    private static final String BLOCK_COLOR = "\033[4;31m";
    private static final String CLEAR_COLOR = "\033[0m";
}
