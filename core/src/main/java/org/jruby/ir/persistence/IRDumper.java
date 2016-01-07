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
import org.jruby.runtime.Signature;
import org.jruby.util.KeyValuePair;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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

    public void visit(IRScope scope, boolean recurse) {
        stream.println("begin " + scope.getScopeType().name() + "<" + scope.getName() + ">");

        scope.prepareForInitialCompilation();
        InterpreterContext ic = scope.getInterpreterContext();

        if (ic.getStaticScope().getSignature() == null) {
            stream.println(Signature.NO_ARGUMENTS);
        } else {
            stream.println(ic.getStaticScope().getSignature());
        }

        Map<String, LocalVariable> localVariables = ic.getScope().getLocalVariables();
        if (localVariables != null && !localVariables.isEmpty()) {
            stream.println("declared variables");
            for (Map.Entry<String, LocalVariable> entry : localVariables.entrySet()) {
                stream.println(VARIABLE_COLOR + "  " + entry.getValue().toString() + CLEAR_COLOR);
            }
        }

        Set<LocalVariable> usedVariables = ic.getScope().getUsedLocalVariables();
        if (usedVariables != null && !usedVariables.isEmpty()) {
            stream.println("used variables");
            for (LocalVariable var : usedVariables) {
                stream.print(VARIABLE_COLOR);
                stream.println("  " + var.toString());
                stream.print(CLEAR_COLOR);
            }
        }

        Instr[] instrs = ic.getInstructions();
        for (int i = 0; i < instrs.length; i++) {
            Instr instr = instrs[i];

            stream.printf("%04d: ", i);
            if (instr instanceof ResultInstr) {
                Variable result = ((ResultInstr) instr).getResult();
                String sigilName = (result instanceof LocalVariable) ? "*" + result.getName() : result.getName();
                stream.printf(VARIABLE_COLOR + "%20s" + CLEAR_COLOR + " := ", sigilName);
            } else {
                stream.printf("%20s    ", "");
            }
            visit(instrs[i]);
            stream.println();
        }

        if (recurse && !scope.getClosures().isEmpty()) {
            stream.println();
            for (IRClosure closure : scope.getClosures()) {
                if (closure == scope) continue;
                visit(closure, true);
            }
        }
    }

    @Override
    public void visit(Instr instr) {
        stream.print(INSTR_COLOR);
        stream.print(instr.getOperation().toString().toLowerCase());
        stream.print(CLEAR_COLOR);
        boolean comma = false;
        for (Operand o : instr.getOperands()) {
            if (!comma) {
                stream.print(INSTR_COLOR);
                stream.print("(");
                stream.print(CLEAR_COLOR);
            }
            if (comma) stream.print(", ");
            comma = true;
            visit(o);
        }
        try {
            Class cls = instr.getClass();
            while (cls != Instr.class) {
                for (Field f : cls.getDeclaredFields()) {
                    if (Modifier.isTransient(f.getModifiers())) continue;
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    if (f.getName().startsWith("$")) continue;
                    if (!comma) {
                        stream.print(INSTR_COLOR);
                        stream.print("(");
                        stream.print(CLEAR_COLOR);
                    }
                    if (comma) stream.print(", ");
                    comma = true;
                    f.setAccessible(true);
                    stream.print(FIELD_COLOR + f.getName() + ": " + CLEAR_COLOR + f.get(instr));
                }
                cls = cls.getSuperclass();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (comma) {
            stream.print(INSTR_COLOR);
            stream.print(")");
            stream.print(CLEAR_COLOR);
        }
    }

    @Override
    public void visit(Operand operand) {
        // Handle variables separately
        if (operand instanceof LocalVariable) {
            stream.print(VARIABLE_COLOR);
            stream.print('*');
            operand.visit(this);
            stream.print(CLEAR_COLOR);
        } else if (operand instanceof TemporaryVariable) {
            stream.print(VARIABLE_COLOR);
            operand.visit(this);
            stream.print(CLEAR_COLOR);
        } else {
            // Other operand forms need type identification
            stream.print(OPERAND_COLOR);
            stream.print(operand.getOperandType().shortName() + "<");
            stream.print(CLEAR_COLOR);
            operand.visit(this);
            stream.print(OPERAND_COLOR);
            stream.print(">");
            stream.print(CLEAR_COLOR);
        }
    }

    public void Array(Array array) {
        final boolean[] comma = {false};
        for (Operand o : Arrays.asList(array.getElts())) {
            if (comma[0]) stream.print(", ");
            comma[1] = true;
            o.visit(this);
        }
    }
    public void AsString(AsString asstring) { visit(asstring.getSource()); }
    public void Bignum(Bignum bignum) { stream.print(bignum.value); }
    public void Boolean(org.jruby.ir.operands.Boolean bool) { stream.print(bool.isTrue() ? "t" : "f"); }
    public void UnboxedBoolean(UnboxedBoolean bool) { stream.print(bool.isTrue() ? "t" : "f"); }
    public void ClosureLocalVariable(ClosureLocalVariable closurelocalvariable) { LocalVariable(closurelocalvariable); }
    public void Complex(Complex complex) { visit(complex.getNumber()); }
    public void CurrentScope(CurrentScope currentscope) { stream.print(currentscope.getScopeNestingDepth()); }
    public void DynamicSymbol(DynamicSymbol dynamicsymbol) { stream.print(dynamicsymbol.getSymbolName()); }
    public void Filename(Filename filename) { }
    public void Fixnum(Fixnum fixnum) { stream.print(fixnum.getValue()); }
    public void FrozenString(FrozenString frozen) { stream.print(frozen.getByteList()); }
    public void UnboxedFixnum(UnboxedFixnum fixnum) { stream.print(fixnum.getValue()); }
    public void Float(org.jruby.ir.operands.Float flote) { stream.print(flote.getValue()); }
    public void UnboxedFloat(org.jruby.ir.operands.UnboxedFloat flote) { stream.print(flote.getValue()); }
    public void GlobalVariable(GlobalVariable globalvariable) { stream.print(globalvariable.getName()); }
    public void Hash(Hash hash) {
        List<KeyValuePair<Operand, Operand>> pairs = hash.getPairs();
        boolean comma = false;
        for (KeyValuePair<Operand, Operand> pair: pairs) {
            if (comma == true) stream.print(',');
            comma = true;
            visit(pair.getKey());
            stream.print("=>");
            visit(pair.getValue());
        }
    }
    public void IRException(IRException irexception) { stream.print(irexception.getType()); }
    public void Label(Label label) { stream.print(label.toString()); }
    public void LocalVariable(LocalVariable localvariable) { stream.print(localvariable.getName()); }
    public void Nil(Nil nil) { }
    public void NthRef(NthRef nthref) { stream.print(nthref.getName()); }
    public void NullBlock(NullBlock nullblock) { }
    public void ObjectClass(ObjectClass objectclass) { }
    public void Rational(Rational rational) { stream.print(rational.getNumerator() + "/" + rational.getDenominator()); }
    public void Regexp(Regexp regexp) { stream.print(regexp.getSource()); }
    public void ScopeModule(ScopeModule scopemodule) { stream.print(scopemodule.getScopeModuleDepth()); }
    public void Self(Self self) { LocalVariable(self); }
    public void Splat(Splat splat) { visit(splat.getArray()); }
    public void StandardError(StandardError standarderror) {  }
    public void StringLiteral(StringLiteral stringliteral) { stream.print(stringliteral.getByteList()); }
    public void SValue(SValue svalue) { visit(svalue.getArray()); }
    public void Symbol(Symbol symbol) { symbol.getName(); }
    public void SymbolProc(SymbolProc symbolproc) { stream.print(symbolproc.getName()); }
    public void TemporaryVariable(TemporaryVariable temporaryvariable) { stream.print(temporaryvariable.getName()); }
    public void TemporaryLocalVariable(TemporaryLocalVariable temporarylocalvariable) { TemporaryVariable(temporarylocalvariable); }
    public void TemporaryFloatVariable(TemporaryFloatVariable temporaryfloatvariable) { TemporaryVariable(temporaryfloatvariable); }
    public void TemporaryFixnumVariable(TemporaryFixnumVariable temporaryfixnumvariable) { TemporaryVariable(temporaryfixnumvariable); }
    public void TemporaryBooleanVariable(TemporaryBooleanVariable temporarybooleanvariable) { TemporaryVariable(temporarybooleanvariable); }
    public void UndefinedValue(UndefinedValue undefinedvalue) {  }
    public void UnexecutableNil(UnexecutableNil unexecutablenil) {  }
    public void WrappedIRClosure(WrappedIRClosure wrappedirclosure) { stream.print(wrappedirclosure.getClosure().getName()); }

    private static final String INSTR_COLOR = "\033[1;36m";
    private static final String OPERAND_COLOR = "\033[1;33m";
    private static final String VARIABLE_COLOR = "\033[1;32m";
    private static final String FIELD_COLOR = "\033[1;34m";
    private static final String CLEAR_COLOR = "\033[0m";
}
