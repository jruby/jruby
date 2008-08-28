require 'test/unit'
require 'compiler/bytecode'

class TestBytecode < Test::Unit::TestCase
  include Compiler::Bytecode

  begin
    import "jruby.objectweb.asm.Opcodes"
  rescue
    import "org.objectweb.asm.Opcodes"
  end
  
  import java.lang.System
  import java.lang.Integer
  import java.lang.Void
  
  class DummyMethod
    def initialize
      @all = []
    end
    def method_missing(sym, *args)
      current = [sym, *args]
      @all << current
      current
    end
    
    def all
      @all
    end
  end
  
  def setup
    @dummy = DummyMethod.new
  end
  
  def method_visitor
    @dummy
  end
  
  def test_var_insns
    assert_equal([:visit_var_insn, Opcodes::ALOAD, :a], (aload :a))
    assert_equal([:visit_var_insn, Opcodes::ILOAD, :a], (iload :a))
    assert_equal([:visit_var_insn, Opcodes::LLOAD, :a], (lload :a))
    assert_equal([:visit_var_insn, Opcodes::FLOAD, :a], (fload :a))
    assert_equal([:visit_var_insn, Opcodes::DLOAD, :a], (dload :a))
    assert_equal([:visit_var_insn, Opcodes::ASTORE, :a], (astore :a))
    assert_equal([:visit_var_insn, Opcodes::ISTORE, :a], (istore :a))
    assert_equal([:visit_var_insn, Opcodes::LSTORE, :a], (lstore :a))
    assert_equal([:visit_var_insn, Opcodes::FSTORE, :a], (fstore :a))
    assert_equal([:visit_var_insn, Opcodes::DSTORE, :a], (dstore :a))
    
    assert_equal([:visit_var_insn, Opcodes::RET, :a], (ret :a))
  end
  
  def test_ldc
    assert_equal([:visit_ldc_insn, "a"], (ldc :a))
    assert_equal([:visit_ldc_insn, "a"], (ldc "a"))
    assert_equal([:visit_ldc_insn, java.lang.Integer.new(1)], (ldc_int 1))
    assert_equal([:visit_ldc_insn, java.lang.Long.new(1)], (ldc_long 1))
    assert_equal([:visit_ldc_insn, java.lang.Float.new(1)], (ldc_float 1))
    assert_equal([:visit_ldc_insn, java.lang.Double.new(1)], (ldc_double 1))
  end

  def test_int_insns
    assert_equal([:visit_int_insn, Opcodes::BIPUSH, 1], (bipush 1))
    assert_equal([:visit_int_insn, Opcodes::SIPUSH, 1], (sipush 1))

    assert_equal([:visit_int_insn, Opcodes::BIPUSH, -2], (push_int(-2)))
    assert_equal([:visit_insn, Opcodes::ICONST_M1], (push_int(-1)))
    assert_equal([:visit_insn, Opcodes::ICONST_0], (push_int(0)))
    assert_equal([:visit_insn, Opcodes::ICONST_1], (push_int(1)))
    assert_equal([:visit_insn, Opcodes::ICONST_2], (push_int(2)))
    assert_equal([:visit_insn, Opcodes::ICONST_3], (push_int(3)))
    assert_equal([:visit_insn, Opcodes::ICONST_4], (push_int(4)))
    assert_equal([:visit_insn, Opcodes::ICONST_5], (push_int(5)))
    assert_equal([:visit_int_insn, Opcodes::BIPUSH, 6], (push_int(6)))

    assert_equal([:visit_int_insn, Opcodes::SIPUSH, -129], (push_int(-129)))
    assert_equal([:visit_int_insn, Opcodes::SIPUSH, 128], (push_int(128)))
    assert_equal([:visit_ldc_insn, -65537], (push_int(-65537)))
    assert_equal([:visit_ldc_insn, 65536], (push_int(65536)))
  end
  
  def test_method_insns
    assert_equal(
      [:visit_method_insn, Opcodes::INVOKESTATIC, "java/lang/Integer", "b", "(Ljava/lang/System;)Ljava/lang/Integer;"],
      (invokestatic Integer, :b, [Integer, System]))
    assert_equal(
      [:visit_method_insn, Opcodes::INVOKESTATIC, "java/lang/Integer", "b", "()Ljava/lang/Integer;"],
      (invokestatic Integer, :b, Integer))
    assert_equal(
      [:visit_method_insn, Opcodes::INVOKEVIRTUAL, "java/lang/Integer", "b", "(Ljava/lang/System;)Ljava/lang/Integer;"],
      (invokevirtual Integer, :b, [Integer, System]))
    assert_equal(
      [:visit_method_insn, Opcodes::INVOKEINTERFACE, "java/lang/Integer", "b", "(Ljava/lang/System;)Ljava/lang/Integer;"],
      (invokeinterface Integer, :b, [Integer, System]))
    assert_equal(
      [:visit_method_insn, Opcodes::INVOKESPECIAL, "java/lang/Integer", "b", "(Ljava/lang/System;)Ljava/lang/Integer;"],
      (invokespecial Integer, :b, [Integer, System]))
  end
  
  def test_return
    assert_equal([:visit_insn, Opcodes::RETURN], (returnvoid))
  end
  
  def test_nop
    assert_equal([:visit_insn, Opcodes::NOP], (nop))
  end

  def test_stack_insns
    assert_equal([:visit_insn, Opcodes::DUP], (dup))
    assert_equal([:visit_insn, Opcodes::SWAP], (swap))
    assert_equal([:visit_insn, Opcodes::POP], (pop))
    assert_equal([:visit_insn, Opcodes::POP2], (pop2))
    assert_equal([:visit_insn, Opcodes::NOP], (nop))
    assert_equal([:visit_insn, Opcodes::DUP_X1], (dup_x1))
    assert_equal([:visit_insn, Opcodes::DUP_X2], (dup_x2))
    assert_equal([:visit_insn, Opcodes::DUP2], (dup2))
    assert_equal([:visit_insn, Opcodes::DUP2_X1], (dup2_x1))
    assert_equal([:visit_insn, Opcodes::DUP2_X2], (dup2_x2))
  end

  def test_arraylength
    assert_equal([:visit_insn, Opcodes::ARRAYLENGTH], (arraylength))
  end

  def test_reference_insns
    assert_equal([:visit_insn, Opcodes::ACONST_NULL], (aconst_null))
    assert_equal([:visit_insn, Opcodes::ARETURN], (areturn))
    assert_equal([:visit_insn, Opcodes::ATHROW], (athrow))
    assert_equal([:visit_insn, Opcodes::AALOAD], (aaload))
    assert_equal([:visit_insn, Opcodes::AASTORE], (aastore))
  end

  def test_byte_insns
    assert_equal([:visit_insn, Opcodes::BALOAD], (baload))
    assert_equal([:visit_insn, Opcodes::BASTORE], (bastore))
  end

  def test_char_insns
    assert_equal([:visit_insn, Opcodes::CALOAD], (caload))
    assert_equal([:visit_insn, Opcodes::CASTORE], (castore))
  end

  def test_short_insns
    assert_equal([:visit_insn, Opcodes::SALOAD], (saload))
    assert_equal([:visit_insn, Opcodes::SASTORE], (sastore))
  end

  def test_int_insns
    assert_equal([:visit_insn, Opcodes::ICONST_M1], (iconst_m1))
    assert_equal([:visit_insn, Opcodes::ICONST_0], (iconst_0))
    assert_equal([:visit_insn, Opcodes::ICONST_1], (iconst_1))
    assert_equal([:visit_insn, Opcodes::ICONST_2], (iconst_2))
    assert_equal([:visit_insn, Opcodes::ICONST_3], (iconst_3))
    assert_equal([:visit_insn, Opcodes::ICONST_4], (iconst_4))
    assert_equal([:visit_insn, Opcodes::ICONST_5], (iconst_5))
    assert_equal([:visit_insn, Opcodes::IALOAD], (iaload))
    assert_equal([:visit_insn, Opcodes::IASTORE], (iastore))
    assert_equal([:visit_insn, Opcodes::IRETURN], (ireturn))
    assert_equal([:visit_insn, Opcodes::IADD], (iadd))
    assert_equal([:visit_insn, Opcodes::ISUB], (isub))
    assert_equal([:visit_insn, Opcodes::IINC], (iinc))
    assert_equal([:visit_insn, Opcodes::IDIV], (idiv))
    assert_equal([:visit_insn, Opcodes::IMUL], (imul))
    assert_equal([:visit_insn, Opcodes::INEG], (ineg))
    assert_equal([:visit_insn, Opcodes::IAND], (iand))
    assert_equal([:visit_insn, Opcodes::IOR], (ior))
    assert_equal([:visit_insn, Opcodes::IXOR], (ixor))
    assert_equal([:visit_insn, Opcodes::IUSHR], (iushr))
    assert_equal([:visit_insn, Opcodes::ISHL], (ishl))
    assert_equal([:visit_insn, Opcodes::ISHR], (ishr))
    assert_equal([:visit_insn, Opcodes::IREM], (irem))
    assert_equal([:visit_insn, Opcodes::I2L], (i2l))
    assert_equal([:visit_insn, Opcodes::I2S], (i2s))
    assert_equal([:visit_insn, Opcodes::I2B], (i2b))
    assert_equal([:visit_insn, Opcodes::I2C], (i2c))
    assert_equal([:visit_insn, Opcodes::I2D], (i2d))
    assert_equal([:visit_insn, Opcodes::I2F], (i2f))
  end

  def test_long_insns
    assert_equal([:visit_insn, Opcodes::LCONST_0], (lconst_0))
    assert_equal([:visit_insn, Opcodes::LCONST_1], (lconst_1))
    assert_equal([:visit_insn, Opcodes::LALOAD], (laload))
    assert_equal([:visit_insn, Opcodes::LASTORE], (lastore))
    assert_equal([:visit_insn, Opcodes::LRETURN], (lreturn))
    assert_equal([:visit_insn, Opcodes::LADD], (ladd))
    assert_equal([:visit_insn, Opcodes::LSUB], (lsub))
    assert_equal([:visit_insn, Opcodes::LDIV], (ldiv))
    assert_equal([:visit_insn, Opcodes::LMUL], (lmul))
    assert_equal([:visit_insn, Opcodes::LNEG], (lneg))
    assert_equal([:visit_insn, Opcodes::LAND], (land))
    assert_equal([:visit_insn, Opcodes::LOR], (lor))
    assert_equal([:visit_insn, Opcodes::LXOR], (lxor))
    assert_equal([:visit_insn, Opcodes::LUSHR], (lushr))
    assert_equal([:visit_insn, Opcodes::LSHL], (lshl))
    assert_equal([:visit_insn, Opcodes::LSHR], (lshr))
    assert_equal([:visit_insn, Opcodes::LREM], (lrem))
    assert_equal([:visit_insn, Opcodes::L2I], (l2i))
    assert_equal([:visit_insn, Opcodes::L2D], (l2d))
    assert_equal([:visit_insn, Opcodes::L2F], (l2f))
  end

  def test_float_insns
    assert_equal([:visit_insn, Opcodes::FALOAD], (faload))
    assert_equal([:visit_insn, Opcodes::FASTORE], (fastore))
    assert_equal([:visit_insn, Opcodes::FRETURN], (freturn))
    assert_equal([:visit_insn, Opcodes::FADD], (fadd))
    assert_equal([:visit_insn, Opcodes::FSUB], (fsub))
    assert_equal([:visit_insn, Opcodes::FDIV], (fdiv))
    assert_equal([:visit_insn, Opcodes::FMUL], (fmul))
    assert_equal([:visit_insn, Opcodes::FNEG], (fneg))
    assert_equal([:visit_insn, Opcodes::FREM], (frem))
    assert_equal([:visit_insn, Opcodes::F2L], (f2l))
    assert_equal([:visit_insn, Opcodes::F2D], (f2d))
    assert_equal([:visit_insn, Opcodes::F2I], (f2i))
  end

  def test_double_insns
    assert_equal([:visit_insn, Opcodes::DALOAD], (daload))
    assert_equal([:visit_insn, Opcodes::DASTORE], (dastore))
    assert_equal([:visit_insn, Opcodes::DRETURN], (dreturn))
    assert_equal([:visit_insn, Opcodes::DADD], (dadd))
    assert_equal([:visit_insn, Opcodes::DSUB], (dsub))
    assert_equal([:visit_insn, Opcodes::DDIV], (ddiv))
    assert_equal([:visit_insn, Opcodes::DMUL], (dmul))
    assert_equal([:visit_insn, Opcodes::DNEG], (dneg))
    assert_equal([:visit_insn, Opcodes::DREM], (drem))
    assert_equal([:visit_insn, Opcodes::D2L], (d2l))
    assert_equal([:visit_insn, Opcodes::D2F], (d2f))
    assert_equal([:visit_insn, Opcodes::D2I], (d2i))
  end

  def test_sync_insns
    assert_equal([:visit_insn, Opcodes::MONITORENTER], (monitorenter))
    assert_equal([:visit_insn, Opcodes::MONITOREXIT], (monitorexit))
  end
  
  def test_type_insns
    assert_equal([:visit_type_insn, Opcodes::NEW, :a], (new :a))
    assert_equal([:visit_type_insn, Opcodes::ANEWARRAY, :a], (anewarray :a))
    assert_equal([:visit_type_insn, Opcodes::NEWARRAY, :a], (newarray :a))
    assert_equal([:visit_type_insn, Opcodes::INSTANCEOF, :a], (instanceof :a))
    assert_equal([:visit_type_insn, Opcodes::CHECKCAST, :a], (checkcast :a))
  end
  
  def test_field_insns
    assert_equal([:visit_field_insn, Opcodes::GETFIELD, "java/lang/Integer", "b", "Ljava/lang/System;"], (getfield Integer, :b, System))
    assert_equal([:visit_field_insn, Opcodes::PUTFIELD, "java/lang/Integer", "b", "Ljava/lang/System;"], (putfield Integer, :b, System))
    assert_equal([:visit_field_insn, Opcodes::GETSTATIC, "java/lang/Integer", "b", "Ljava/lang/System;"], (getstatic Integer, :b, System))
    assert_equal([:visit_field_insn, Opcodes::PUTSTATIC, "java/lang/Integer", "b", "Ljava/lang/System;"], (putstatic Integer, :b, System))
  end
  
  def test_trycatch
    assert_equal([:visit_try_catch_block, :a, :b, :c, :d], (trycatch :a, :b, :c, :d))
  end
  
  def test_jump_insns
    lbl = label
    assert_equal([:visit_jump_insn, Opcodes::GOTO, lbl.label], (goto lbl))
    assert_equal([:visit_jump_insn, Opcodes::IFEQ, lbl.label], (ifeq lbl))
    assert_equal([:visit_jump_insn, Opcodes::IFNE, lbl.label], (ifne lbl))
    assert_equal([:visit_jump_insn, Opcodes::IFLE, lbl.label], (ifle lbl))
    assert_equal([:visit_jump_insn, Opcodes::IFLT, lbl.label], (iflt lbl))
    assert_equal([:visit_jump_insn, Opcodes::IFGE, lbl.label], (ifge lbl))
    assert_equal([:visit_jump_insn, Opcodes::IFGT, lbl.label], (ifgt lbl))
    assert_equal([:visit_jump_insn, Opcodes::IF_ACMPEQ, lbl.label], (if_acmpeq lbl))
    assert_equal([:visit_jump_insn, Opcodes::IF_ACMPNE, lbl.label], (if_acmpne lbl))
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPEQ, lbl.label], (if_icmpeq lbl))
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPNE, lbl.label], (if_icmpne lbl))
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPLT, lbl.label], (if_icmplt lbl))
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPGT, lbl.label], (if_icmpgt lbl))
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPLE, lbl.label], (if_icmple lbl))
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPGE, lbl.label], (if_icmpge lbl))
    assert_equal([:visit_jump_insn, Opcodes::IFNULL, lbl.label], (ifnull lbl))
    assert_equal([:visit_jump_insn, Opcodes::IFNONNULL, lbl.label], (ifnonnull lbl))
    assert_equal([:visit_jump_insn, Opcodes::JSR, lbl.label], (jsr lbl))
  end

  def test_multidim_array
    assert_equal([:visit_multi_anew_array_insn, "Ljava/lang/Integer;", 5], (multianewarray Integer, 5))
  end
  
  def test_lookup_switch
    assert_equal([:visit_lookup_switch_insn, :a, :b, :c], (lookupswitch :a, :b, :c))
  end
  
  def test_table_switch
    assert_equal([:visit_table_switch_insn, :a, :b, :c, :d], (tableswitch :a, :b, :c, :d))
  end
  
  def test_label
    l1 = label
    assert_equal([:visit_label, l1.label], l1.set!)
  end
  
  def test_aprintln
    aprintln
    assert_equal(
      [ [:visit_insn, Opcodes::DUP],
        [:visit_field_insn, Opcodes::GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"],
        [:visit_insn, Opcodes::SWAP],
        [ :visit_method_insn,
          Opcodes::INVOKEVIRTUAL,
          "java/io/PrintStream",
          "println",
          "(Ljava/lang/Object;)V"]], @dummy.all)
  end
  
  def test_swap2
    swap2
    assert_equal([[:visit_insn, Opcodes::DUP2_X2], [:visit_insn, Opcodes::POP2]], @dummy.all)
  end
end