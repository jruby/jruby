package org.jruby.ir.persistence;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.jruby.RubyLocalJumpError.Reason;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.AsString;
import org.jruby.ir.operands.Backref;
import org.jruby.ir.operands.BacktickString;
import org.jruby.ir.operands.Bignum;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.ClosureLocalVariable;
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
import org.jruby.ir.operands.Range;
import org.jruby.ir.operands.Regexp;
import org.jruby.ir.operands.SValue;
import org.jruby.ir.operands.ScopeModule;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.StandardError;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.UnexecutableNil;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.util.KCode;
import org.jruby.util.RegexpOptions;

public enum IROperandFactory {
    INSTANCE;

    /** Array:[$operands] */
    public Array createArray(Operand[] operands) {
        return new Array(operands);
    }

    /** #{$source} */
    public AsString createAsString(Operand source) {
        return new AsString(source);
    }

    /** \$$name (e.g. $a) */
    public Backref createBackref(String name) {
        char t = name.charAt(0);
        return new Backref(t);
    }

    public BacktickString createBacktickString(Operand[] pieces) {
        return new BacktickString(Arrays.asList(pieces));
    }

    /** $bignumString:bignum */
    public Bignum createBignum(String bignumString) {
        BigInteger value = new BigInteger(bignumString);
        return new Bignum(value);
    }

    /** true|false */
    public BooleanLiteral createBooleanLiteral(String booleanLiteralString) {
        boolean truthy = Boolean.parseBoolean(booleanLiteralString);
        return new BooleanLiteral(truthy);
    }

    public BooleanLiteral createTrueLiteral() {
        return new BooleanLiteral(true);
    }

    public BooleanLiteral createFalseLiteral() {
        return new BooleanLiteral(false);
    }

    /** <$name($scopeDepthString:$locationString)> */
    public ClosureLocalVariable createClosureLocalVariable(IRClosure scope, String name,
            String scopeDepthString, String locationString) {
        int scopeDepth = Integer.parseInt(scopeDepthString);
        int location = Integer.parseInt(locationString);
        return new ClosureLocalVariable(scope, name, scopeDepth, location);
    }

    /** ArgsPush:[$a1, $a2] */
    public CompoundArray createArgsPush(Operand a1, Operand a2) {
        return new CompoundArray(a1, a2, true);
    }

    /** ArgsCat:[$a1, $a2] */
    public CompoundArray createArgsCat(Operand a1, Operand a2) {
        return new CompoundArray(a1, a2, false);
    }

    /** CompoundString:$pieces */
    public CompoundString createCompoundString(List<Operand> pieces) {
        return new CompoundString(pieces);
    }

    /** scope<$name> */
    public CurrentScope createCurrentScope(IRScope scope, String name) {
        return new CurrentScope(scope);
    }

    /** :CompoundString$pieces */
    public DynamicSymbol createDynamicSymbol(CompoundString n) {
        return new DynamicSymbol(n);
    }

    /** Fixnum:$valueString */
    public Fixnum createFixnum(String valueString) {
        Long value = Long.valueOf(valueString);
        return new Fixnum(value);
    }

    /** Float:$valueString */
    public org.jruby.ir.operands.Float createFloat(String valueString) {
        Double value = Double.valueOf(valueString);
        return new Float(value);
    }

    public GlobalVariable createGlobalVariable(String name) {
        return new GlobalVariable(name);
    }

    public Hash createHash(KeyValuePair[] pairs) {
        return new Hash(Arrays.asList(pairs));
    }

    /** $key => $value */
    public KeyValuePair createKeyValuePair(Operand key, Operand value) {
        return new KeyValuePair(key, value);
    }

    public IRException createIRException(String type) {
        Reason reason = Reason.valueOf(type.toUpperCase());
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
            // what is that?
            return null;
        }
    }

    /** $label */
    public Label createLabel(String label) {
        return new Label(label);
    }

    /** $name($scopeDepthString:$locationString) */
    public LocalVariable createLocalVariable(String name, String scopeDepthString,
            String locationString) {
        int scopeDepth = Integer.parseInt(scopeDepthString);
        int location = Integer.parseInt(locationString);
        return new LocalVariable(name, scopeDepth, location);
    }

    /** $name */
    public MethAddr createMethAddr(String name) {
        return new MethAddr(name);
    }

    /** <$receiver.$methodName> */
    public MethodHandle createMethodHandle(Operand methodName, Operand receiver) {
        return new MethodHandle(methodName, receiver);
    }

    /** nil */
    public Nil createNil() {
        return new Nil();
    }

    /** \$matchNumber */
    public NthRef createNthRef(String matchNumberString) {
        int matchNumber = Integer.parseInt(matchNumberString);
        return new NthRef(matchNumber);
    }

    /** <Class:Object> */
    public ObjectClass createObjectClass() {
        return new ObjectClass();
    }

    /** Range:($begin...$end) */
    public Range createInclusiveRange(Operand begin, Operand end) {
        return new Range(begin, end, false);
    }

    /** Range:($begin..$end) */
    public Range createExclusiveRange(Operand begin, Operand end) {
        return new Range(begin, end, true);
    }

    /** RE:|$regexp|$options */
    public Regexp createRegexp(Operand regexp, RegexpOptions options) {
        return new Regexp(regexp, options);
    }

    /**
     * RegexpOptions(kcode:$kcode(, encodingNone)?(, extended)?(, fixed)?(,
     * ignorecase)?(, java)?(, kcodeDefault)?(, literal)?(, multiline)?(,
     * once)?)
     */
    public RegexpOptions createRegexpOptions(String kcodeString, String[] options) {
        KCode kCode = KCode.valueOf(kcodeString);

        if (options != null) {
            List<String> optionList = Arrays.asList(options);

            boolean isKCodeDefault = false;
            if (optionList.contains("kcodeDefault")) {
                isKCodeDefault = true;
                // already used
                optionList.remove("kcodeDefault");
            }
            RegexpOptions result = new RegexpOptions(kCode, isKCodeDefault);

            for (String option : optionList) {
                if ("encodingNone".equals(option)) {
                    result.setEncodingNone(true);
                } else if ("extended".equals(option)) {
                    result.setExtended(true);
                } else if ("fixed".equals(option)) {
                    result.setFixed(true);
                } else if ("ignorecase".equals(option)) {
                    result.setIgnorecase(true);
                } else if ("java".equals(option)) {
                    result.setJava(true);
                } else if ("literal".equals(option)) {
                    result.setLiteral(true);
                } else if ("multiline".equals(option)) {
                    result.setMultiline(true);
                } else if ("once".equals(option)) {
                    result.setOnce(true);
                }
            }

            return result;
        } else {
            return new RegexpOptions(kCode, false);
        }

    }

    /** module<$name> */
    public ScopeModule createScopeModule(IRScope scope, String name) {
        return new ScopeModule(scope);
    }

    /** %self */
    public Self createSelf() {
        return Self.SELF;
    }

    /** *$array */
    public Splat createSplat(Operand array) {
        return new Splat(array);
    }

    /** StandardError */
    public StandardError createStandardError() {
        return new StandardError();
    }

    /** "$s" */
    public StringLiteral createStringLiteral(String s) {
        return new StringLiteral(s);
    }

    /** SValue:($array) */
    public SValue createSValue(Operand array) {
        return new SValue(array);
    }

    /** :$name */
    public Symbol createSymbol(String name) {
        return new Symbol(name);
    }

    /** %undefined */
    public UndefinedValue createUndefininedValue() {
        return UndefinedValue.UNDEFINED;
    }

    /** nil(unexecutable) */
    public UnexecutableNil createUnexecutableNil() {
        return UnexecutableNil.U_NIL;
    }

    public WrappedIRClosure createWrappedIRClosure(IRClosure scope) {
        return new WrappedIRClosure(scope);
    }

}
