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

    def single
      old, @all = @all, []
      yield
      result, @all = @all, old
      result[0]
    end
  end
  
  def setup
    @dummy = DummyMethod.new
  end
  
  def method_visitor
    @dummy
  end
  
  def test_var_insns
    assert_equal([:visit_var_insn, Opcodes::ALOAD, :a], @dummy.single {aload :a})
    assert_equal([:visit_var_insn, Opcodes::ILOAD, :a], @dummy.single {iload :a})
    assert_equal([:visit_var_insn, Opcodes::LLOAD, :a], @dummy.single {lload :a})
    assert_equal([:visit_var_insn, Opcodes::FLOAD, :a], @dummy.single {fload :a})
    assert_equal([:visit_var_insn, Opcodes::DLOAD, :a], @dummy.single {dload :a})
    assert_equal([:visit_var_insn, Opcodes::ASTORE, :a], @dummy.single {astore :a})
    assert_equal([:visit_var_insn, Opcodes::ISTORE, :a], @dummy.single {istore :a})
    assert_equal([:visit_var_insn, Opcodes::LSTORE, :a], @dummy.single {lstore :a})
    assert_equal([:visit_var_insn, Opcodes::FSTORE, :a], @dummy.single {fstore :a})
    assert_equal([:visit_var_insn, Opcodes::DSTORE, :a], @dummy.single {dstore :a})
    
    assert_equal([:visit_var_insn, Opcodes::RET, :a], @dummy.single {ret :a})
  end
  
  def test_ldc
    assert_equal([:visit_ldc_insn, "a"], @dummy.single {ldc :a})
    assert_equal([:visit_ldc_insn, "a"], @dummy.single {ldc "a"})
    assert_equal([:visit_ldc_insn, java.lang.Integer.new(1)], @dummy.single {ldc_int 1})
    assert_equal([:visit_ldc_insn, java.lang.Long.new(1)], @dummy.single {ldc_long 1})
    assert_equal([:visit_ldc_insn, java.lang.Float.new(1)], @dummy.single {ldc_float 1})
    assert_equal([:visit_ldc_insn, java.lang.Double.new(1)], @dummy.single {ldc_double 1})
  end

  def test_int_insns
    assert_equal([:visit_int_insn, Opcodes::BIPUSH, 1], @dummy.single {bipush 1})
    assert_equal([:visit_int_insn, Opcodes::SIPUSH, 1], @dummy.single {sipush 1})

    assert_equal([:visit_int_insn, Opcodes::BIPUSH, -2], @dummy.single {push_int(-2)})
    assert_equal([:visit_insn, Opcodes::ICONST_M1], @dummy.single {push_int(-1)})
    assert_equal([:visit_insn, Opcodes::ICONST_0], @dummy.single {push_int(0)})
    assert_equal([:visit_insn, Opcodes::ICONST_1], @dummy.single {push_int(1)})
    assert_equal([:visit_insn, Opcodes::ICONST_2], @dummy.single {push_int(2)})
    assert_equal([:visit_insn, Opcodes::ICONST_3], @dummy.single {push_int(3)})
    assert_equal([:visit_insn, Opcodes::ICONST_4], @dummy.single {push_int(4)})
    assert_equal([:visit_insn, Opcodes::ICONST_5], @dummy.single {push_int(5)})
    assert_equal([:visit_int_insn, Opcodes::BIPUSH, 6], @dummy.single {push_int(6)})

    assert_equal([:visit_int_insn, Opcodes::SIPUSH, -129], @dummy.single {push_int(-129)})
    assert_equal([:visit_int_insn, Opcodes::SIPUSH, 128], @dummy.single {push_int(128)})
    assert_equal([:visit_ldc_insn, -65537], @dummy.single {push_int(-65537)})
    assert_equal([:visit_ldc_insn, 65536], @dummy.single {push_int(65536)})
  end
  
  def test_method_insns
    assert_equal(
      [:visit_method_insn, Opcodes::INVOKESTATIC, "java/lang/Integer", "b", "(Ljava/lang/System;)Ljava/lang/Integer;"],
      @dummy.single {invokestatic Integer, :b, [Integer, System]})
    assert_equal(
      [:visit_method_insn, Opcodes::INVOKESTATIC, "java/lang/Integer", "b", "()Ljava/lang/Integer;"],
      @dummy.single {invokestatic Integer, :b, Integer})
    assert_equal(
      [:visit_method_insn, Opcodes::INVOKEVIRTUAL, "java/lang/Integer", "b", "(Ljava/lang/System;)Ljava/lang/Integer;"],
      @dummy.single {invokevirtual Integer, :b, [Integer, System]})
    assert_equal(
      [:visit_method_insn, Opcodes::INVOKEINTERFACE, "java/lang/Integer", "b", "(Ljava/lang/System;)Ljava/lang/Integer;"],
      @dummy.single {invokeinterface Integer, :b, [Integer, System]})
    assert_equal(
      [:visit_method_insn, Opcodes::INVOKESPECIAL, "java/lang/Integer", "b", "(Ljava/lang/System;)Ljava/lang/Integer;"],
      @dummy.single {invokespecial Integer, :b, [Integer, System]})
  end
  
  def test_return
    assert_equal([:visit_insn, Opcodes::RETURN], @dummy.single {returnvoid})
  end
  
  def test_nop
    assert_equal([:visit_insn, Opcodes::NOP], @dummy.single {nop})
  end

  def test_stack_insns
    assert_equal([:visit_insn, Opcodes::DUP], @dummy.single {dup})
    assert_equal([:visit_insn, Opcodes::SWAP], @dummy.single {swap})
    assert_equal([:visit_insn, Opcodes::POP], @dummy.single {pop})
    assert_equal([:visit_insn, Opcodes::POP2], @dummy.single {pop2})
    assert_equal([:visit_insn, Opcodes::DUP_X1], @dummy.single {dup_x1})
    assert_equal([:visit_insn, Opcodes::DUP_X2], @dummy.single {dup_x2})
    assert_equal([:visit_insn, Opcodes::DUP2], @dummy.single {dup2})
    assert_equal([:visit_insn, Opcodes::DUP2_X1], @dummy.single {dup2_x1})
    assert_equal([:visit_insn, Opcodes::DUP2_X2], @dummy.single {dup2_x2})
  end

  def test_arraylength
    assert_equal([:visit_insn, Opcodes::ARRAYLENGTH], @dummy.single {arraylength})
  end

  def test_reference_insns
    assert_equal([:visit_insn, Opcodes::ACONST_NULL], @dummy.single {aconst_null})
    assert_equal([:visit_insn, Opcodes::ARETURN], @dummy.single {areturn})
    assert_equal([:visit_insn, Opcodes::ATHROW], @dummy.single {athrow})
    assert_equal([:visit_insn, Opcodes::AALOAD], @dummy.single {aaload})
    assert_equal([:visit_insn, Opcodes::AASTORE], @dummy.single {aastore})
  end

  def test_byte_insns
    assert_equal([:visit_insn, Opcodes::BALOAD], @dummy.single {baload})
    assert_equal([:visit_insn, Opcodes::BASTORE], @dummy.single {bastore})
  end

  def test_char_insns
    assert_equal([:visit_insn, Opcodes::CALOAD], @dummy.single {caload})
    assert_equal([:visit_insn, Opcodes::CASTORE], @dummy.single {castore})
  end

  def test_short_insns
    assert_equal([:visit_insn, Opcodes::SALOAD], @dummy.single {saload})
    assert_equal([:visit_insn, Opcodes::SASTORE], @dummy.single {sastore})
  end

  def test_int_insns
    assert_equal([:visit_insn, Opcodes::ICONST_M1], @dummy.single {iconst_m1})
    assert_equal([:visit_insn, Opcodes::ICONST_0], @dummy.single {iconst_0})
    assert_equal([:visit_insn, Opcodes::ICONST_1], @dummy.single {iconst_1})
    assert_equal([:visit_insn, Opcodes::ICONST_2], @dummy.single {iconst_2})
    assert_equal([:visit_insn, Opcodes::ICONST_3], @dummy.single {iconst_3})
    assert_equal([:visit_insn, Opcodes::ICONST_4], @dummy.single {iconst_4})
    assert_equal([:visit_insn, Opcodes::ICONST_5], @dummy.single {iconst_5})
    assert_equal([:visit_insn, Opcodes::IALOAD], @dummy.single {iaload})
    assert_equal([:visit_insn, Opcodes::IASTORE], @dummy.single {iastore})
    assert_equal([:visit_insn, Opcodes::IRETURN], @dummy.single {ireturn})
    assert_equal([:visit_insn, Opcodes::IADD], @dummy.single {iadd})
    assert_equal([:visit_insn, Opcodes::ISUB], @dummy.single {isub})
    assert_equal([:visit_insn, Opcodes::IINC], @dummy.single {iinc})
    assert_equal([:visit_insn, Opcodes::IDIV], @dummy.single {idiv})
    assert_equal([:visit_insn, Opcodes::IMUL], @dummy.single {imul})
    assert_equal([:visit_insn, Opcodes::INEG], @dummy.single {ineg})
    assert_equal([:visit_insn, Opcodes::IAND], @dummy.single {iand})
    assert_equal([:visit_insn, Opcodes::IOR], @dummy.single {ior})
    assert_equal([:visit_insn, Opcodes::IXOR], @dummy.single {ixor})
    assert_equal([:visit_insn, Opcodes::IUSHR], @dummy.single {iushr})
    assert_equal([:visit_insn, Opcodes::ISHL], @dummy.single {ishl})
    assert_equal([:visit_insn, Opcodes::ISHR], @dummy.single {ishr})
    assert_equal([:visit_insn, Opcodes::IREM], @dummy.single {irem})
    assert_equal([:visit_insn, Opcodes::I2L], @dummy.single {i2l})
    assert_equal([:visit_insn, Opcodes::I2S], @dummy.single {i2s})
    assert_equal([:visit_insn, Opcodes::I2B], @dummy.single {i2b})
    assert_equal([:visit_insn, Opcodes::I2C], @dummy.single {i2c})
    assert_equal([:visit_insn, Opcodes::I2D], @dummy.single {i2d})
    assert_equal([:visit_insn, Opcodes::I2F], @dummy.single {i2f})
  end

  def test_long_insns
    assert_equal([:visit_insn, Opcodes::LCONST_0], @dummy.single {lconst_0})
    assert_equal([:visit_insn, Opcodes::LCONST_1], @dummy.single {lconst_1})
    assert_equal([:visit_insn, Opcodes::LALOAD], @dummy.single {laload})
    assert_equal([:visit_insn, Opcodes::LASTORE], @dummy.single {lastore})
    assert_equal([:visit_insn, Opcodes::LRETURN], @dummy.single {lreturn})
    assert_equal([:visit_insn, Opcodes::LADD], @dummy.single {ladd})
    assert_equal([:visit_insn, Opcodes::LSUB], @dummy.single {lsub})
    assert_equal([:visit_insn, Opcodes::LDIV], @dummy.single {ldiv})
    assert_equal([:visit_insn, Opcodes::LMUL], @dummy.single {lmul})
    assert_equal([:visit_insn, Opcodes::LNEG], @dummy.single {lneg})
    assert_equal([:visit_insn, Opcodes::LAND], @dummy.single {land})
    assert_equal([:visit_insn, Opcodes::LOR], @dummy.single {lor})
    assert_equal([:visit_insn, Opcodes::LXOR], @dummy.single {lxor})
    assert_equal([:visit_insn, Opcodes::LUSHR], @dummy.single {lushr})
    assert_equal([:visit_insn, Opcodes::LSHL], @dummy.single {lshl})
    assert_equal([:visit_insn, Opcodes::LSHR], @dummy.single {lshr})
    assert_equal([:visit_insn, Opcodes::LREM], @dummy.single {lrem})
    assert_equal([:visit_insn, Opcodes::L2I], @dummy.single {l2i})
    assert_equal([:visit_insn, Opcodes::L2D], @dummy.single {l2d})
    assert_equal([:visit_insn, Opcodes::L2F], @dummy.single {l2f})
  end

  def test_float_insns
    assert_equal([:visit_insn, Opcodes::FCONST_0], @dummy.single {fconst_0})
    assert_equal([:visit_insn, Opcodes::FCONST_1], @dummy.single {fconst_1})
    assert_equal([:visit_insn, Opcodes::FCONST_2], @dummy.single {fconst_2})
    assert_equal([:visit_insn, Opcodes::FALOAD], @dummy.single {faload})
    assert_equal([:visit_insn, Opcodes::FASTORE], @dummy.single {fastore})
    assert_equal([:visit_insn, Opcodes::FRETURN], @dummy.single {freturn})
    assert_equal([:visit_insn, Opcodes::FADD], @dummy.single {fadd})
    assert_equal([:visit_insn, Opcodes::FSUB], @dummy.single {fsub})
    assert_equal([:visit_insn, Opcodes::FDIV], @dummy.single {fdiv})
    assert_equal([:visit_insn, Opcodes::FMUL], @dummy.single {fmul})
    assert_equal([:visit_insn, Opcodes::FNEG], @dummy.single {fneg})
    assert_equal([:visit_insn, Opcodes::FREM], @dummy.single {frem})
    assert_equal([:visit_insn, Opcodes::F2L], @dummy.single {f2l})
    assert_equal([:visit_insn, Opcodes::F2D], @dummy.single {f2d})
    assert_equal([:visit_insn, Opcodes::F2I], @dummy.single {f2i})
  end

  def test_double_insns
    assert_equal([:visit_insn, Opcodes::DCONST_0], @dummy.single {dconst_0})
    assert_equal([:visit_insn, Opcodes::DCONST_1], @dummy.single {dconst_1})
    assert_equal([:visit_insn, Opcodes::DALOAD], @dummy.single {daload})
    assert_equal([:visit_insn, Opcodes::DASTORE], @dummy.single {dastore})
    assert_equal([:visit_insn, Opcodes::DRETURN], @dummy.single {dreturn})
    assert_equal([:visit_insn, Opcodes::DADD], @dummy.single {dadd})
    assert_equal([:visit_insn, Opcodes::DSUB], @dummy.single {dsub})
    assert_equal([:visit_insn, Opcodes::DDIV], @dummy.single {ddiv})
    assert_equal([:visit_insn, Opcodes::DMUL], @dummy.single {dmul})
    assert_equal([:visit_insn, Opcodes::DNEG], @dummy.single {dneg})
    assert_equal([:visit_insn, Opcodes::DREM], @dummy.single {drem})
    assert_equal([:visit_insn, Opcodes::D2L], @dummy.single {d2l})
    assert_equal([:visit_insn, Opcodes::D2F], @dummy.single {d2f})
    assert_equal([:visit_insn, Opcodes::D2I], @dummy.single {d2i})
  end

  def test_sync_insns
    assert_equal([:visit_insn, Opcodes::MONITORENTER], @dummy.single {monitorenter})
    assert_equal([:visit_insn, Opcodes::MONITOREXIT], @dummy.single {monitorexit})
  end
  
  def test_type_insns
    assert_equal([:visit_type_insn, Opcodes::NEW, :a], @dummy.single {new :a})
    assert_equal([:visit_type_insn, Opcodes::ANEWARRAY, :a], @dummy.single {anewarray :a})
    assert_equal([:visit_type_insn, Opcodes::NEWARRAY, :a], @dummy.single {newarray :a})
    assert_equal([:visit_type_insn, Opcodes::INSTANCEOF, :a], @dummy.single {instanceof :a})
    assert_equal([:visit_type_insn, Opcodes::CHECKCAST, :a], @dummy.single {checkcast :a})
  end
  
  def test_field_insns
    assert_equal([:visit_field_insn, Opcodes::GETFIELD, "java/lang/Integer", "b", "Ljava/lang/System;"], @dummy.single {getfield Integer, :b, System})
    assert_equal([:visit_field_insn, Opcodes::PUTFIELD, "java/lang/Integer", "b", "Ljava/lang/System;"], @dummy.single {putfield Integer, :b, System})
    assert_equal([:visit_field_insn, Opcodes::GETSTATIC, "java/lang/Integer", "b", "Ljava/lang/System;"], @dummy.single {getstatic Integer, :b, System})
    assert_equal([:visit_field_insn, Opcodes::PUTSTATIC, "java/lang/Integer", "b", "Ljava/lang/System;"], @dummy.single {putstatic Integer, :b, System})
  end
  
  def test_trycatch
    assert_equal([:visit_try_catch_block, :a, :b, :c, :d], @dummy.single {trycatch :a, :b, :c, :d})
  end
  
  def test_jump_insns
    lbl = label
    assert_equal([:visit_jump_insn, Opcodes::GOTO, lbl.label], @dummy.single {goto lbl})
    assert_equal([:visit_jump_insn, Opcodes::IFEQ, lbl.label], @dummy.single {ifeq lbl})
    assert_equal([:visit_jump_insn, Opcodes::IFNE, lbl.label], @dummy.single {ifne lbl})
    assert_equal([:visit_jump_insn, Opcodes::IFLE, lbl.label], @dummy.single {ifle lbl})
    assert_equal([:visit_jump_insn, Opcodes::IFLT, lbl.label], @dummy.single {iflt lbl})
    assert_equal([:visit_jump_insn, Opcodes::IFGE, lbl.label], @dummy.single {ifge lbl})
    assert_equal([:visit_jump_insn, Opcodes::IFGT, lbl.label], @dummy.single {ifgt lbl})
    assert_equal([:visit_jump_insn, Opcodes::IF_ACMPEQ, lbl.label], @dummy.single {if_acmpeq lbl})
    assert_equal([:visit_jump_insn, Opcodes::IF_ACMPNE, lbl.label], @dummy.single {if_acmpne lbl})
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPEQ, lbl.label], @dummy.single {if_icmpeq lbl})
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPNE, lbl.label], @dummy.single {if_icmpne lbl})
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPLT, lbl.label], @dummy.single {if_icmplt lbl})
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPGT, lbl.label], @dummy.single {if_icmpgt lbl})
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPLE, lbl.label], @dummy.single {if_icmple lbl})
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPGE, lbl.label], @dummy.single {if_icmpge lbl})
    assert_equal([:visit_jump_insn, Opcodes::IFNULL, lbl.label], @dummy.single {ifnull lbl})
    assert_equal([:visit_jump_insn, Opcodes::IFNONNULL, lbl.label], @dummy.single {ifnonnull lbl})
    assert_equal([:visit_jump_insn, Opcodes::JSR, lbl.label], @dummy.single {jsr lbl})
  end

  def test_multidim_array
    assert_equal([:visit_multi_anew_array_insn, "Ljava/lang/Integer;", 5], @dummy.single {multianewarray Integer, 5})
  end
  
  def test_lookup_switch
    assert_equal([:visit_lookup_switch_insn, :a, :b, :c], @dummy.single {lookupswitch :a, :b, :c})
  end
  
  def test_table_switch
    assert_equal([:visit_table_switch_insn, :a, :b, :c, :d], @dummy.single {tableswitch :a, :b, :c, :d})
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
  
  def test_var_insns_deltas
    assert_equal(1, (aload :a))
    assert_equal(1, (iload :a))
    assert_equal(2, (lload :a))
    assert_equal(1, (fload :a))
    assert_equal(2, (dload :a))
    assert_equal(-1, (astore :a))
    assert_equal(-1, (istore :a))
    assert_equal(-2, (lstore :a))
    assert_equal(-1, (fstore :a))
    assert_equal(-2, (dstore :a))
    
    assert_equal(0, (ret :a))
  end
  
  def test_ldc_deltas
    assert_equal(1, (ldc :a))
    assert_equal(1, (ldc "a"))
    assert_equal(1, (ldc_int 1))
    assert_equal(2, (ldc_long 1))
    assert_equal(1, (ldc_float 1))
    assert_equal(2, (ldc_double 1))
  end

  def test_int_insns_deltas
    assert_equal(1, (bipush 1))
    assert_equal(1, (sipush 1))

    assert_equal(1, (push_int(-2)))
    assert_equal(1, (push_int(-1)))
    assert_equal(1, (push_int(0)))
    assert_equal(1, (push_int(1)))
    assert_equal(1, (push_int(2)))
    assert_equal(1, (push_int(3)))
    assert_equal(1, (push_int(4)))
    assert_equal(1, (push_int(5)))
    assert_equal(1, (push_int(6)))

    assert_equal(1, (push_int(-129)))
    assert_equal(1, (push_int(128)))
    assert_equal(1, (push_int(-65537)))
    assert_equal(1, (push_int(65536)))
  end
  
  def test_method_insns_deltas
    assert_equal(
      0,
      (invokestatic Integer, :b, [Integer, System]))
    assert_equal(
      1,
      (invokestatic Integer, :b, Integer))
    assert_equal(
      -1,
      (invokevirtual Integer, :b, [Integer, System]))
    assert_equal(
      -1,
      (invokeinterface Integer, :b, [Integer, System]))
    assert_equal(
      -1,
      (invokespecial Integer, :b, [Integer, System]))
  end
  
  def test_return_deltas
    assert_equal(0, (returnvoid))
  end
  
  def test_nop_deltas
    assert_equal(0, (nop))
  end

  def test_stack_insns_deltas
    assert_equal(1, (dup))
    assert_equal(0, (swap))
    assert_equal(-1, (pop))
    assert_equal(-2, (pop2))
    assert_equal(1, (dup_x1))
    assert_equal(1, (dup_x2))
    assert_equal(2, (dup2))
    assert_equal(2, (dup2_x1))
    assert_equal(2, (dup2_x2))
  end

  def test_arraylength_deltas
    assert_equal(0, (arraylength))
  end

  def test_reference_insns_deltas
    assert_equal(1, (aconst_null))
    assert_equal(-1, (areturn))
    assert_equal(-1, (athrow))
    assert_equal(-1, (aaload))
    assert_equal(-3, (aastore))
  end

  def test_byte_insns_deltas
    assert_equal(-1, (baload))
    assert_equal(-3, (bastore))
  end

  def test_char_insns_deltas
    assert_equal(-1, (caload))
    assert_equal(-3, (castore))
  end

  def test_short_insns_deltas
    assert_equal(-1, (saload))
    assert_equal(-3, (sastore))
  end

  def test_int_insns_deltas
    assert_equal(1, (iconst_m1))
    assert_equal(1, (iconst_0))
    assert_equal(1, (iconst_1))
    assert_equal(1, (iconst_2))
    assert_equal(1, (iconst_3))
    assert_equal(1, (iconst_4))
    assert_equal(1, (iconst_5))
    assert_equal(-1, (iaload))
    assert_equal(-3, (iastore))
    assert_equal(-1, (ireturn))
    assert_equal(-1, (iadd))
    assert_equal(-1, (isub))
    assert_equal(0, (iinc))
    assert_equal(-1, (idiv))
    assert_equal(-1, (imul))
    assert_equal(0, (ineg))
    assert_equal(-1, (iand))
    assert_equal(-1, (ior))
    assert_equal(-1, (ixor))
    assert_equal(0, (iushr))
    assert_equal(0, (ishl))
    assert_equal(0, (ishr))
    assert_equal(-1, (irem))
    assert_equal(1, (i2l))
    assert_equal(0, (i2s))
    assert_equal(0, (i2b))
    assert_equal(0, (i2c))
    assert_equal(1, (i2d))
    assert_equal(0, (i2f))
  end

  def test_long_insns_deltas
    assert_equal(2, (lconst_0))
    assert_equal(2, (lconst_1))
    assert_equal(0, (laload))
    assert_equal(-4, (lastore))
    assert_equal(-2, (lreturn))
    assert_equal(-2, (ladd))
    assert_equal(-2, (lsub))
    assert_equal(-2, (ldiv))
    assert_equal(-2, (lmul))
    assert_equal(0, (lneg))
    assert_equal(-2, (land))
    assert_equal(-2, (lor))
    assert_equal(-2, (lxor))
    assert_equal(0, (lushr))
    assert_equal(0, (lshl))
    assert_equal(0, (lshr))
    assert_equal(-2, (lrem))
    assert_equal(-1, (l2i))
    assert_equal(0, (l2d))
    assert_equal(-1, (l2f))
  end

  def test_float_insns_deltas
    assert_equal(1, (fconst_0))
    assert_equal(1, (fconst_1))
    assert_equal(1, (fconst_2))
    assert_equal(-1, (faload))
    assert_equal(-3, (fastore))
    assert_equal(-1, (freturn))
    assert_equal(-1, (fadd))
    assert_equal(-1, (fsub))
    assert_equal(-1, (fdiv))
    assert_equal(-1, (fmul))
    assert_equal(0, (fneg))
    assert_equal(-1, (frem))
    assert_equal(1, (f2l))
    assert_equal(1, (f2d))
    assert_equal(0, (f2i))
  end

  def test_double_insns_deltas
    assert_equal(2, (dconst_0))
    assert_equal(2, (dconst_1))
    assert_equal(0, (daload))
    assert_equal(-4, (dastore))
    assert_equal(-2, (dreturn))
    assert_equal(-2, (dadd))
    assert_equal(-2, (dsub))
    assert_equal(-2, (ddiv))
    assert_equal(-2, (dmul))
    assert_equal(0, (dneg))
    assert_equal(-2, (drem))
    assert_equal(0, (d2l))
    assert_equal(-1, (d2f))
    assert_equal(-1, (d2i))
  end

  def test_sync_insns_deltas
    assert_equal(-1, (monitorenter))
    assert_equal(-1, (monitorexit))
  end
  
  def test_type_insns_deltas
    assert_equal(1, (new :a))
    assert_equal(0, (anewarray :a))
    assert_equal(0, (newarray :a))
    assert_equal(0, (instanceof :a))
    assert_equal(0, (checkcast :a))
  end
  
  def test_field_insns_deltas
    assert_equal(0, (getfield Integer, :b, System))
    assert_equal(-2, (putfield Integer, :b, System))
    assert_equal(1, (getstatic Integer, :b, System))
    assert_equal(-1, (putstatic Integer, :b, System))
  end
  
  def test_jump_insns_deltas
    lbl = label
    assert_equal(0, (goto lbl))
    assert_equal(-1, (ifeq lbl))
    assert_equal(-1, (ifne lbl))
    assert_equal(-1, (ifle lbl))
    assert_equal(-1, (iflt lbl))
    assert_equal(-1, (ifge lbl))
    assert_equal(-1, (ifgt lbl))
    assert_equal(-2, (if_acmpeq lbl))
    assert_equal(-2, (if_acmpne lbl))
    assert_equal(-2, (if_icmpeq lbl))
    assert_equal(-2, (if_icmpne lbl))
    assert_equal(-2, (if_icmplt lbl))
    assert_equal(-2, (if_icmpgt lbl))
    assert_equal(-2, (if_icmple lbl))
    assert_equal(-2, (if_icmpge lbl))
    assert_equal(-1, (ifnull lbl))
    assert_equal(-1, (ifnonnull lbl))
    assert_equal(1, (jsr lbl))
  end

  def test_multidim_array_deltas
    assert_equal(-5, (multianewarray Integer, 5))
  end
  
  def test_lookup_switch_deltas
    assert_equal(-1, (lookupswitch :a, :b, :c))
  end
  
  def test_table_switch_deltas
    assert_equal(-1, (tableswitch :a, :b, :c, :d))
  end
end