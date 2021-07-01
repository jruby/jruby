/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubySymbol;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.*;
import org.jruby.ir.instructions.defined.GetErrorInfoInstr;
import org.jruby.ir.instructions.defined.RestoreErrorInfoInstr;
import org.jruby.ir.operands.*;
import org.jruby.parser.StaticScope;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.Signature;
import org.jruby.util.ByteList;

import static com.headius.backport9.buffer.Buffers.positionBuffer;

/**
 *
 * @author enebo
 */
public class IRReaderStream implements IRReaderDecoder, IRPersistenceValues {
    private final ByteBuffer buf;
    private final IRManager manager;
    private final List<IRScope> scopes;
    private IRScope currentScope; // FIXME: This is not thread-safe and more than a little gross
    /** Filename to use for the script */
    private final ByteList filename;
    private RubySymbol[] constantPool;

    public IRReaderStream(IRManager manager, byte[] bytes, ByteList filename) {
        this(ByteBuffer.wrap(bytes), manager, new ArrayList<>(), null, filename, null);
    }

    public IRReaderStream(IRManager manager, File file, ByteList filename) {
        this(readingIntoBuffer(file), manager, new ArrayList<>(), null, filename, null);
    }

    private IRReaderStream(ByteBuffer buf, IRManager manager, List<IRScope> scopes, IRScope currentScope, ByteList filename, RubySymbol[] constantPool) {
        this.buf = buf;
        this.manager = manager;
        this.scopes = scopes;
        this.currentScope = currentScope;
        this.filename = filename;
        this.constantPool = constantPool;
    }

    public IRReaderDecoder dup() {
        return new IRReaderStream(buf.duplicate(), manager, new ArrayList(scopes), currentScope, filename, constantPool);
    }

    private static ByteBuffer readingIntoBuffer(File file) {
        try {
            return ByteBuffer.wrap(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            Logger.getLogger(IRReaderStream.class.getName()).log(Level.SEVERE, null, e);
        }
        return null;
    }

    @Override
    public ByteList getFilename() {
        return filename;
    }

    @Override
    public ByteList decodeByteList() {
        return new ByteList(decodeByteArray(), decodeEncoding());
    }

    @Override
    public byte[] decodeByteArray() {
        int size = decodeInt();
        byte[] bytes = new byte[size];
        buf.get(bytes);
        return bytes;
    }

    @Override
    public Encoding decodeEncoding() {
        int size = decodeInt();

        if (size == USASCII) {
            return USASCIIEncoding.INSTANCE;
        } else if (size == UTF8) {
            return UTF8Encoding.INSTANCE;
        } else {
            // FIXME: Since we are looking up on byte[] we can avoid alloc by keeping temp array around (this is very uncommon though)
            byte[] encodingName = new byte[size];
            buf.get(encodingName);
            return EncodingDB.getEncodings().get(encodingName).getEncoding();
        }
    }

    @Override
    public Label decodeLabel() {
        return (Label) decodeOperand();
    }

    @Override
    public Label[] decodeLabelArray() {
        int size = decodeInt();
        Label[] labels = new Label[size];
        for (int i = 0; i < size; i++) {
            labels[i] = decodeLabel();
        }

        return labels;
    }

    @Override
    public RubyEvent decodeRubyEvent() {
        return RubyEvent.fromOrdinal(decodeInt());
    }

    private RubySymbol decodeSymbolFromConstantPool() {
        int strLength = decodeInt();

        if (strLength == NULL_STRING) return null;

        byte[] bytes = new byte[strLength]; // FIXME: This seems really innefficient
        buf.get(bytes);

        Encoding encoding = decodeEncoding();

        return manager.getRuntime().newSymbol(new ByteList(bytes, encoding, false));
    }

    @Override
    public RubySymbol decodeSymbol() {
        int poolIndex = decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("INDEX: " + poolIndex);

        return constantPool[poolIndex];
    }

    @Override
    public String decodeString() {
        int strLength = decodeInt();

        if (strLength == NULL_STRING) return null;

        byte[] bytes = new byte[strLength]; // FIXME: This seems really innefficient
        buf.get(bytes);

        String newString = new String(bytes).intern();

        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("STR<" + newString + ">");

        return newString;
    }

    @Override
    public void addScope(IRScope scope) {
        scopes.add(scope);
        currentScope = scope;
    }

    @Override
    public IRScope getCurrentScope() {
        return currentScope;
    }

    @Override
    public String[] decodeStringArray() {
        int arrayLength = decodeInt();
        String[] array = new String[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            array[i] = decodeString();
        }
        return array;
    }

    @Override
    public int[] decodeIntArray() {
        int size = decodeInt();
        int[] ints = new int[size];
        for (int i = 0; i < size; i++) {
            ints[i] = decodeInt();
        }
        return ints;
    }

    private Map<String, Operand> vars = null;

    // Labels use this to make sure they share the same instances
    @Override
    public Map<String, Operand> getVars() {
        return vars;
    }

    private void decodeConstantPool(int offset) {
        positionBuffer(buf, offset);
        int size = decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("DECODING " + size + " symbols");

        constantPool = new RubySymbol[size];
        for (int i = 0; i < size; i++) {
            constantPool[i] = decodeSymbolFromConstantPool();
            if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("SYM: " + constantPool[i]);
        }
    }

    @Override
    public List<Instr> decodeInstructionsAt(IRScope scope, int poolOffset, int offset) {
        decodeConstantPool(poolOffset);
        currentScope = scope;
        vars = new HashMap<>();
        positionBuffer(buf, offset);

        int numberOfInstructions = decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("Number of Instructions: " + numberOfInstructions);
        List<Instr> instrs = new ArrayList<>(numberOfInstructions);

        for (int i = 0; i < numberOfInstructions; i++) {
            Instr decodedInstr = decodeInstr();

            if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println(">INSTR = " + decodedInstr);

            instrs.add(decodedInstr);
        }

        return instrs;
    }

    public Instr decodeInstr() {
        Operation operation = decodeOperation();

        switch (operation) {
            case ALIAS: return AliasInstr.decode(this);
            case ARRAY_DEREF: return ArrayDerefInstr.decode(this);
            case ATTR_ASSIGN: return AttrAssignInstr.decode(this);
            case AS_STRING: return AsStringInstr.decode(this);
            case B_FALSE: return BFalseInstr.decode(this);
            case B_NIL: return BNilInstr.decode(this);
            case B_SWITCH: return BSwitchInstr.decode(this);
            case B_TRUE: return BTrueInstr.decode(this);
            case B_UNDEF: return BUndefInstr.decode(this);
            case BINDING_LOAD: return LoadLocalVarInstr.decode(this);
            case BINDING_STORE: return StoreLocalVarInstr.decode(this);
            case BLOCK_GIVEN: return BlockGivenInstr.decode(this);
            case BNE: return BNEInstr.decode(this);
            case BREAK: return BreakInstr.decode(this);
            case BUILD_BACKREF: return BuildBackrefInstr.decode(this);
            case BUILD_COMPOUND_ARRAY: return BuildCompoundArrayInstr.decode(this);
            case BUILD_COMPOUND_STRING: return BuildCompoundStringInstr.decode(this);
            case BUILD_DREGEXP: return BuildDynRegExpInstr.decode(this);
            case BUILD_RANGE: return BuildRangeInstr.decode(this);
            case BUILD_SPLAT: return BuildSplatInstr.decode(this);
            case CALL_1F:
            case CALL_1D:
            case CALL_1O:
            case CALL_2O:
            case CALL_1OB:
            case CALL_0O:
            case CALL: return CallInstr.decode(this);
            case CHECK_ARGS_ARRAY_ARITY: return CheckArgsArrayArityInstr.decode(this);
            case CHECK_ARITY: return CheckArityInstr.decode(this);
            case CHECK_FOR_LJE: return CheckForLJEInstr.decode(this);
            case CLASS_SUPER: return ClassSuperInstr.decode(this);
            case CLASS_VAR_MODULE: return GetClassVarContainerModuleInstr.decode(this);
            case COPY: return CopyInstr.decode(this);
            case DEF_CLASS: return DefineClassInstr.decode(this);
            case DEF_CLASS_METH: return DefineClassMethodInstr.decode(this);
            case DEF_INST_METH: return DefineInstanceMethodInstr.decode(this);
            case DEF_META_CLASS: return DefineMetaClassInstr.decode(this);
            case DEF_MODULE: return DefineModuleInstr.decode(this);
            case EQQ: return EQQInstr.decode(this);
            case EXC_REGION_END: return new ExceptionRegionEndMarkerInstr();
            case EXC_REGION_START: return ExceptionRegionStartMarkerInstr.decode(this);
            case GET_CVAR: return GetClassVariableInstr.decode(this);
            case GET_ENCODING: return GetEncodingInstr.decode(this);
            case GET_ERROR_INFO: return GetErrorInfoInstr.decode(this);
            case GET_FIELD: return GetFieldInstr.decode(this);
            case GET_GLOBAL_VAR: return GetGlobalVariableInstr.decode(this);
            case GVAR_ALIAS: return GVarAliasInstr.decode(this);
            case INHERITANCE_SEARCH_CONST: return InheritanceSearchConstInstr.decode(this);
            case INSTANCE_SUPER: return InstanceSuperInstr.decode(this);
            case JUMP: return JumpInstr.decode(this);
            case LABEL: return LabelInstr.decode(this);
            case LAMBDA: return BuildLambdaInstr.decode(this);
            case LEXICAL_SEARCH_CONST: return LexicalSearchConstInstr.decode(this);
            case LOAD_FRAME_CLOSURE: return LoadFrameClosureInstr.decode(this);
            case LOAD_BLOCK_IMPLICIT_CLOSURE: return LoadBlockImplicitClosureInstr.decode(this);
            case LOAD_IMPLICIT_CLOSURE: return LoadImplicitClosureInstr.decode(this);
            case LINE_NUM: return LineNumberInstr.decode(this);
            case MASGN_OPT: return OptArgMultipleAsgnInstr.decode(this);
            case MASGN_REQD: return ReqdArgMultipleAsgnInstr.decode(this);
            case MASGN_REST: return RestArgMultipleAsgnInstr.decode(this);
            case MATCH: return MatchInstr.decode(this);
            case NONLOCAL_RETURN: return NonlocalReturnInstr.decode(this);
            case NOP: return NopInstr.NOP;
            case NORESULT_CALL:
            case NORESULT_CALL_1O: return NoResultCallInstr.decode(this);
            case POP_BINDING: return PopBindingInstr.decode(this);
            case POP_METHOD_FRAME: return PopMethodFrameInstr.decode(this);
            case PROCESS_MODULE_BODY: return ProcessModuleBodyInstr.decode(this);
            case PUSH_METHOD_BINDING: return PushMethodBindingInstr.decode(this);
            case PUSH_METHOD_FRAME: return PushMethodFrameInstr.decode(this);
            case PUT_CONST: return PutConstInstr.decode(this);
            case PUT_CVAR: return PutClassVariableInstr.decode(this);
            case PUT_FIELD: return PutFieldInstr.decode(this);
            case PUT_GLOBAL_VAR: return PutGlobalVarInstr.decode(this);
            case RAISE_ARGUMENT_ERROR: return RaiseArgumentErrorInstr.decode(this);
            case RAISE_REQUIRED_KEYWORD_ARGUMENT_ERROR: return RaiseRequiredKeywordArgumentError.decode(this);
            case RECORD_END_BLOCK: return RecordEndBlockInstr.decode(this);
            case REIFY_CLOSURE: return ReifyClosureInstr.decode(this);
            case RECV_RUBY_EXC: return ReceiveRubyExceptionInstr.decode(this);
            case RECV_JRUBY_EXC: return ReceiveJRubyExceptionInstr.decode(this);
            case RECV_KW_ARG: return ReceiveKeywordArgInstr.decode(this);
            case RECV_KW_REST_ARG: return ReceiveKeywordRestArgInstr.decode(this);
            case RECV_OPT_ARG: return ReceiveOptArgInstr.decode(this);
            case RECV_POST_REQD_ARG: return ReceivePostReqdArgInstr.decode(this);
            case RECV_PRE_REQD_ARG: return ReceivePreReqdArgInstr.decode(this);
            case RECV_REST_ARG: return ReceiveRestArgInstr.decode(this);
            case RECV_SELF: return ReceiveSelfInstr.decode(this);
            case RESCUE_EQQ: return RescueEQQInstr.decode(this);
            case RESTORE_ERROR_INFO: return RestoreErrorInfoInstr.decode(this);
            case RETURN: return ReturnInstr.decode(this);
            case RETURN_OR_RETHROW_SAVED_EXC: return ReturnOrRethrowSavedExcInstr.decode(this);
            case RUNTIME_HELPER: return RuntimeHelperCall.decode(this);
            case SEARCH_CONST: return SearchConstInstr.decode(this);
            case SEARCH_MODULE_FOR_CONST: return SearchModuleForConstInstr.decode(this);
            case SET_CAPTURED_VAR: return SetCapturedVarInstr.decode(this);
            case TRACE: return TraceInstr.decode(this);
            case THREAD_POLL: return ThreadPollInstr.decode(this);
            case THROW: return ThrowExceptionInstr.decode(this);
            case TO_ARY: return ToAryInstr.decode(this);
            case TOGGLE_BACKTRACE: return ToggleBacktraceInstr.decode(this);
            case UNDEF_METHOD: return UndefMethodInstr.decode(this);
            case UNRESOLVED_SUPER: return UnresolvedSuperInstr.decode(this);
            case YIELD: return YieldInstr.decode(this);
            case ZSUPER: return ZSuperInstr.decode(this);
        }

        throw new IllegalArgumentException("Unhandled operation: " + operation);
    }    

    @Override
    public IRScopeType decodeIRScopeType() {
        return IRScopeType.fromOrdinal(decodeInt());
    }

    @Override
    public TemporaryVariableType decodeTemporaryVariableType() {
        return TemporaryVariableType.fromOrdinal(decodeInt());
    }

    @Override
    public StaticScope.Type decodeStaticScopeType() {
        return StaticScope.Type.fromOrdinal(decodeInt());
    }

    @Override
    public Operation decodeOperation() {
        Operation operation = Operation.fromOrdinal(decodeInt());
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("INSTR<" + operation);
        return operation;
    }

    @Override
    public Operand decodeOperand() {
        OperandType operandType = decodeOperandType();

        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("OP<" + operandType);

        Operand decodedOperand = decode(operandType);

        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println(">OP = " + decodedOperand);

        return decodedOperand;
    }

    @Override
    public Variable decodeVariable() {
        return (Variable) decodeOperand();
    }

    @Override
    public Operand[] decodeOperandArray() {
        int size = decodeInt();
        Operand[] list = new Operand[size];

        for (int i = 0; i < size; i++) {
            list[i] = decodeOperand();
        }

        return list;
    }

    @Override
    public List<Operand> decodeOperandList() {
        int size = decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("OPERAND LIST of size: " + size);
        List<Operand> list = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("OPERAND #" + i);
            list.add(decodeOperand());
        }

        return list;
    }

    @Override
    public OperandType decodeOperandType() {
        return OperandType.fromCoded(decodeByte());
    }

    @Override
    public boolean decodeBoolean() {
        byte value = buf.get();
        if (value == TRUE) return true;
        if (value == FALSE) return false;

        throw new IllegalArgumentException("Value (" + ((int) value) + ", " + (char) value + ") is not a boolean.");
    }

    @Override
    public byte decodeByte() {
        return buf.get();
    }

    @Override
    public char decodeChar() {
        return buf.getChar();
    }

    @Override
    public int decodeInt() {
        byte b = buf.get();
        return b == FULL ? buf.getInt() : (int) b;
    }

    @Override
    public int decodeIntRaw() {
        return buf.getInt();
    }

    @Override
    public long decodeLong() {
        byte b = buf.get();
        return b == FULL ? buf.getLong() : (int) b;
    }

    @Override
    public double decodeDouble() {
        return buf.getDouble();
    }

    @Override
    public float decodeFloat() {
        return buf.getFloat();
    }

    @Override
    public IRScope decodeScope() {
        return scopes.get(decodeInt());
    }

    @Override
    public Signature decodeSignature() {
        return Signature.decode(decodeLong());
    }

    @Override
    public EnumSet<IRFlags> decodeIRFlags() {
        EnumSet<IRFlags> flags = EnumSet.noneOf(IRFlags.class);
        IRFlags[] values = IRFlags.values();

        for (int value = decodeInt(); value != 0; value ^= Integer.lowestOneBit(value)) {
            int index = Integer.numberOfTrailingZeros(value);
            flags.add(values[index]);
        }

        return flags;
    }

    @Override
    public void seek(int headersOffset) {
        positionBuffer(buf, headersOffset);
    }

    public Operand decode(OperandType type) {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("Decoding operand " + type);

        switch (type) {
            case ARRAY: return Array.decode(this);
            case BIGNUM: return Bignum.decode(this);
            case BOOLEAN: return org.jruby.ir.operands.Boolean.decode(this);
            case COMPLEX: return Complex.decode(this);
            case CURRENT_SCOPE: return CurrentScope.decode(this);
            case DYNAMIC_SYMBOL: return DynamicSymbol.decode(this);
            case FILENAME: return Filename.decode(this);
            case FIXNUM: return Fixnum.decode(this);
            case FLOAT: return org.jruby.ir.operands.Float.decode(this);
            case FROZEN_STRING: return FrozenString.decode(this);
            case GLOBAL_VARIABLE: return GlobalVariable.decode(this);
            case HASH: return Hash.decode(this);
            case IR_EXCEPTION: return IRException.decode(this);
            case LABEL: return Label.decode(this);
            case LOCAL_VARIABLE: return LocalVariable.decode(this);
            case NIL: return manager.getNil();
            case NTH_REF: return NthRef.decode(this);
            case NULL_BLOCK: return NullBlock.decode(this);
            case OBJECT_CLASS: return new ObjectClass();
            case RATIONAL: return Rational.decode(this);
            case REGEXP: return Regexp.decode(this);
            case SCOPE_MODULE: return ScopeModule.decode(this);
            case SELF: return Self.SELF;
            case SPLAT: return Splat.decode(this);
            case STANDARD_ERROR: return new StandardError();
            case STRING_LITERAL: return MutableString.decode(this);
            case SVALUE: return SValue.decode(this);
            case SYMBOL: return Symbol.decode(this);
            case SYMBOL_PROC: return SymbolProc.decode(this);
            case TEMPORARY_VARIABLE: return TemporaryLocalVariable.decode(this);
            case UNBOXED_BOOLEAN: return new UnboxedBoolean(decodeBoolean());
            case UNBOXED_FIXNUM: return new UnboxedFixnum(decodeLong());
            case UNBOXED_FLOAT: return new UnboxedFloat(decodeDouble());
            case UNDEFINED_VALUE: return UndefinedValue.UNDEFINED;
            case UNEXECUTABLE_NIL: return UnexecutableNil.U_NIL;
            case WRAPPED_IR_CLOSURE: return WrappedIRClosure.decode(this);
        }

        return null;
    }
}
