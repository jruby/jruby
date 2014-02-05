package org.jruby.ir.persistence.read.parser;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jcodings.Encoding;
import org.jruby.RubyLocalJumpError.Reason;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.AsString;
import org.jruby.ir.operands.Backref;
import org.jruby.ir.operands.BacktickString;
import org.jruby.ir.operands.Bignum;
import org.jruby.ir.operands.UnboxedBoolean;
import org.jruby.ir.operands.CompoundArray;
import org.jruby.ir.operands.CompoundString;
import org.jruby.ir.operands.CurrentScope;
import org.jruby.ir.operands.DynamicSymbol;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Float;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Hash;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.KeyValuePair;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.MethodHandle;
import org.jruby.ir.operands.Nil;
import org.jruby.ir.operands.NthRef;
import org.jruby.ir.operands.ObjectClass;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.OperandType;
import org.jruby.ir.operands.Range;
import org.jruby.ir.operands.Regexp;
import org.jruby.ir.operands.SValue;
import org.jruby.ir.operands.ScopeModule;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.StandardError;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.UnboxedFixnum;
import org.jruby.ir.operands.UnboxedFloat;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.UnexecutableNil;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.util.KCode;
import org.jruby.util.RegexpOptions;

public class IROperandFactory {

    private final IRParsingContext context;

    public IROperandFactory(IRParsingContext context) {
        this.context = context;
    }

    public Operand createOperandWithoutParameters(final String operandName) {
        final OperandType operandType = NonIRObjectFactory.createOperandType(operandName);

        switch (operandType) {
        case NIL:
            return createNil();
        case OBJECT_CLASS:
            return createObjectClass();
        case SELF:
            return createSelf();
        case STANDARD_ERROR:
            return createStandardError();
        case UNDEFINED_VALUE:
            return createUndefininedValue();
        case UNEXECUTABLE_NIL:
            return createUnexecutableNil();
        default:
            throw new UnsupportedOperationException(operandName);
        }
    }

    private Nil createNil() {
        final IRManager irManager = context.getIRManager();
        return irManager.getNil();
    }

    private ObjectClass createObjectClass() {
        return new ObjectClass();
    }

    private Self createSelf() {
        return Self.SELF;
    }

    private StandardError createStandardError() {
        // FIXME? No instantiation of StandardError found. Not needed?
        return new StandardError();
    }

    private UndefinedValue createUndefininedValue() {
        return UndefinedValue.UNDEFINED;
    }

    private UnexecutableNil createUnexecutableNil() {
        return UnexecutableNil.U_NIL;
    }

    public Operand createOperandWithParameters(final String operandName, final List<Object> parameters) {

        final OperandType operandType = NonIRObjectFactory.createOperandType(operandName);

        final ParametersIterator parametersIterator = new ParametersIterator(context, parameters);

        switch (operandType) {
        case ARRAY:
            return createArray(parametersIterator);
        case AS_STRING:
            return createAsString(parametersIterator);
        case BACKREF:
            return createBackref(parametersIterator);
        case BACKTICK_STRING:
            return createBacktickString(parametersIterator);
        case BIGNUM:
            return createBignum(parametersIterator);
        case BOOLEAN_LITERAL:
            return createBooleanLiteral(parametersIterator);
        case COMPOUND_ARRAY:
            return createCompoundArray(parametersIterator);
        case COMPOUND_STRING:
            return createCompoundString(parametersIterator);
        case CURRENT_SCOPE:
            return createCurrentScope(parametersIterator);
        case DYNAMIC_SYMBOL:
            return createDynamicSymbol(parametersIterator);
        case FIXNUM:
            return createFixnum(parametersIterator);
        case UNBOXED_FIXNUM:
            return createUnboxedFixnum(parametersIterator);
        case FLOAT:
            return createFloat(parametersIterator);
        case UNBOXED_FLOAT:
            return createUnboxedFloat(parametersIterator);
        case GLOBAL_VARIABLE:
            return createGlobalVariable(parametersIterator);
        case HASH:
            return createHash(parametersIterator);
        case IR_EXCEPTION:
            return createIRException(parametersIterator);
        case LABEL:
            return createLabel(parametersIterator);
        case LOCAL_VARIABLE:
            return createLocalVariable(parametersIterator);
        case METH_ADDR:
            return createMethAddr(parametersIterator);
        case METHOD_HANDLE:
            return createMethodHandle(parametersIterator);
        case NTH_REF:
            return createNthRef(parametersIterator);
        case RANGE:
            return createRange(parametersIterator);
        case REGEXP:
            return createRegexp(parametersIterator);
        case SCOPE_MODULE:
            return createScopeModule(parametersIterator);
        case SPLAT:
            return createSplat(parametersIterator);
        case STRING_LITERAL:
            return createStringLiteral(parametersIterator);
        case SVALUE:
            return createSValue(parametersIterator);
        case SYMBOL:
            return createSymbol(parametersIterator);
        case TEMPORARY_VARIABLE:
            return createTemporaryVariable(parametersIterator);
        case WRAPPED_IR_CLOSURE:
            return createWrappedIRClosure(parametersIterator);
        default:
            throw new UnsupportedOperationException(operandName);
        }
    }

    private Array createArray(final ParametersIterator parametersIterator) {
        final Operand[] operands = parametersIterator.nextOperandArray();

        return new Array(operands);
    }

    private AsString createAsString(final ParametersIterator parametersIterator) {
        final Operand source = parametersIterator.nextOperand();

        return new AsString(source);
    }

    private Backref createBackref(final ParametersIterator parametersIterator) {
        final String typeString = parametersIterator.nextString();
        final char t = typeString.charAt(0);

        return new Backref(t);
    }

    private BacktickString createBacktickString(final ParametersIterator parametersIterator) {
        final List<Operand> pieces = parametersIterator.nextOperandList();

        return new BacktickString(pieces);
    }

    private Bignum createBignum(final ParametersIterator parametersIterator) {
        final String bignumString = parametersIterator.nextString();
        final BigInteger value = new BigInteger(bignumString);

        return new Bignum(value);
    }

    private UnboxedBoolean createBooleanLiteral(final ParametersIterator parametersIterator) {
        final IRManager irManager = context.getIRManager();
        final boolean isTrue = parametersIterator.nextBoolean();

        return isTrue ? irManager.getTrue() : irManager.getFalse();
    }

    private CompoundArray createCompoundArray(final ParametersIterator parametersIterator) {
        final Operand a1 = parametersIterator.nextOperand();
        final Operand a2 = parametersIterator.nextOperand();
        final boolean argsPush = parametersIterator.nextBoolean();

        return new CompoundArray(a1, a2, argsPush);
    }

    private CompoundString createCompoundString(final ParametersIterator parametersIterator) {
        final List<Operand> pieces = parametersIterator.nextOperandList();

        final String encodingName = parametersIterator.nextString();
        final Encoding encoding = NonIRObjectFactory.createEncoding(encodingName);

        return new CompoundString(pieces, encoding);
    }

    private CurrentScope createCurrentScope(final ParametersIterator parametersIterator) {
        final IRScope scope = parametersIterator.nextScope();

        return new CurrentScope(scope);
    }

    private DynamicSymbol createDynamicSymbol(final ParametersIterator parametersIterator) {
        final CompoundString compoundString = (CompoundString) parametersIterator.next();

        return new DynamicSymbol(compoundString);
    }

    private Fixnum createFixnum(final ParametersIterator parametersIterator) {
        final String valueString = parametersIterator.nextString();
        final Long value = Long.valueOf(valueString);

        return new Fixnum(value);
    }

    private UnboxedFixnum createUnboxedFixnum(final ParametersIterator parametersIterator) {
        final String valueString = parametersIterator.nextString();
        final Long value = Long.valueOf(valueString);

        return new UnboxedFixnum(value);
    }

    private org.jruby.ir.operands.Float createFloat(final ParametersIterator parametersIterator) {
        final String valueString = parametersIterator.nextString();
        final Double value = Double.valueOf(valueString);
        return new Float(value);
    }

    private org.jruby.ir.operands.UnboxedFloat createUnboxedFloat(final ParametersIterator parametersIterator) {
        final String valueString = parametersIterator.nextString();
        final Double value = Double.valueOf(valueString);
        return new UnboxedFloat(value);
    }

    private GlobalVariable createGlobalVariable(final ParametersIterator parametersIterator) {
        final String name = parametersIterator.nextString();

        return new GlobalVariable(name);
    }

    private Hash createHash(final ParametersIterator parametersIterator) {
        final List<KeyValuePair> pairs = new ArrayList<KeyValuePair>();

        final Iterator<Object> keyValuePairsIterator = parametersIterator.nextList().iterator();
        while (keyValuePairsIterator.hasNext()) {
            @SuppressWarnings("unchecked")
            final List<Operand> keyValuePair = (List<Operand>) keyValuePairsIterator.next();
            final Iterator<Operand> keyValuePairIterator = keyValuePair.iterator();

            final Operand key = keyValuePairIterator.next();
            final Operand value = keyValuePairIterator.next();

            KeyValuePair pair = new KeyValuePair(key, value);

            pairs.add(pair);
        }

        return new Hash(pairs);
    }

    private IRException createIRException(final ParametersIterator parametersIterator) {
        final String reasonString = parametersIterator.nextString();
        final Reason reason = NonIRObjectFactory.createReason(reasonString);

        switch (reason) {
        case BREAK:
            return IRException.BREAK_LocalJumpError;
        case NEXT:
            return IRException.NEXT_LocalJumpError;
        case REDO:
            return IRException.REDO_LocalJumpError;
        case RETRY:
            return IRException.RETRY_LocalJumpError;
        case RETURN:
            return IRException.RETURN_LocalJumpError;
        default:
            throw new UnsupportedOperationException(reasonString);
        }
    }

    private Label createLabel(final ParametersIterator parametersIterator) {
        final String labelName = parametersIterator.nextString();

        // Special case of label
        if("_GLOBAL_ENSURE_BLOCK".equals(labelName)) {
            return new Label("_GLOBAL_ENSURE_BLOCK", 0);
        }

        // Check if this label was already created
        // Important! Program would not be interpreted correctly
        // if new name will be created every time
        if(context.isContainsLabel(labelName)) {
            return context.getLabel(labelName);
        }

        // FIXME? Warning! This code is relies on current realization of IRScope#getNewLable
        // which constructs name in format '${prefix}_\d+'
        // so '_\d+' is removed here and newly recreated label will have the same name
        // with one that was persisted
        final int lastIndexOfPrefix = labelName.lastIndexOf("_");
        final int lastIndexNotFound = -1;
        String prefix = labelName;
        if(lastIndexOfPrefix != lastIndexNotFound) {
            prefix = labelName.substring(0, lastIndexOfPrefix);
        }
        final IRScope currentScope = context.getCurrentScope();

        Label newLabel = currentScope.getNewLabel(prefix);

        // Add to context for future reuse
        context.addLabel(labelName, newLabel);

        return newLabel;
    }

    private LocalVariable createLocalVariable(final ParametersIterator parametersIterator) {
        final String name = parametersIterator.nextString();
        final int scopeDepth = parametersIterator.nextInt();

        return context.getCurrentScope().getLocalVariable(name, scopeDepth);
    }

    private MethAddr createMethAddr(final ParametersIterator parametersIterator) {
        final String name = parametersIterator.nextString();

        // Special cases
        if(MethAddr.NO_METHOD.getName().equals(name)) {
            return MethAddr.NO_METHOD;
        } else if (MethAddr.UNKNOWN_SUPER_TARGET.getName().equals(name)){
            return MethAddr.UNKNOWN_SUPER_TARGET;
        }

        return new MethAddr(name);
    }

    private MethodHandle createMethodHandle(final ParametersIterator parametersIterator) {
        final Operand methodName = parametersIterator.nextOperand();
        final Operand receiver = parametersIterator.nextOperand();

        return new MethodHandle(methodName, receiver);
    }

    private NthRef createNthRef(final ParametersIterator parametersIterator) {
        return new NthRef(parametersIterator.nextInt());
    }

    private Range createRange(final ParametersIterator parametersIterator) {
        final Operand begin = parametersIterator.nextOperand();
        final Operand end = parametersIterator.nextOperand();
        final boolean isExclusive = parametersIterator.nextBoolean();

        return new Range(begin, end, isExclusive);
    }

    private Regexp createRegexp(final ParametersIterator parametersIterator) {
        final Operand regexp = parametersIterator.nextOperand();
        final String kcodeName = parametersIterator.nextString();
        final boolean isKCodeDefault = parametersIterator.nextBoolean();
        final KCode kcode = NonIRObjectFactory.createKcode(kcodeName);

        final RegexpOptions options = new RegexpOptions(kcode, isKCodeDefault);

        return new Regexp(regexp, options);
    }

    private ScopeModule createScopeModule(final ParametersIterator parametersIterator) {
        final IRScope scope = parametersIterator.nextScope();

        return new ScopeModule(scope);
    }

    private Splat createSplat(final ParametersIterator parametersIterator) {
        final Operand array = parametersIterator.nextOperand();

        return new Splat(array);
    }

    private StringLiteral createStringLiteral(final ParametersIterator parametersIterator) {
        final String s = parametersIterator.nextString();

        return new StringLiteral(s);
    }

    private SValue createSValue(final ParametersIterator parametersIterator) {
        final Operand array = parametersIterator.nextOperand();

        return new SValue(array);
    }

    private Symbol createSymbol(final ParametersIterator parametersIterator) {
        return new Symbol(parametersIterator.nextString());
    }

    private Variable createTemporaryVariable(final ParametersIterator parametersIterator) {

        final String name = parametersIterator.nextString();

        final IRScope currentScope = context.getCurrentScope();

/*        if (Variable.CURRENT_SCOPE.equals(name)) {
            return currentScope.getCurrentScopeVariable();
        } else if (Variable.CURRENT_MODULE.equals(name)) {
            return currentScope.getCurrentModuleVariable();
        } else if (context.isContainsVariable(name)) {
            return context.getVariable(name);
        } else {
            final TemporaryLocalVariable newTemporaryVariable = currentScope.getNewTemporaryVariable(name);
            context.addVariable(newTemporaryVariable);
            return newTemporaryVariable;
        }*/
        return null;
    }

    private WrappedIRClosure createWrappedIRClosure(final ParametersIterator parametersIterator) {
        final IRClosure closure = (IRClosure) parametersIterator.nextScope();

        // FIXME: Unlikely this is correct
        return new WrappedIRClosure(context.getCurrentScope().getNewTemporaryVariable(), closure);
    }

}
