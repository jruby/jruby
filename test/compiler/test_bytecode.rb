require 'test/unit'
require 'compiler/bytecode'

class TestBytecode < Test::Unit::TestCase
  include Compiler::Bytecode
  
  import "jruby.objectweb.asm.Opcodes"
  import java.lang.System
  
  class DummyMethod
    def method_missing(sym, *args)
      [sym, *args]
    end
  end
  
  def method_visitor
    DummyMethod.new
  end
  
  def test_var_insns
    assert_equal([:visit_var_insn, Opcodes::ALOAD, :a], (aload :a))
    assert_equal([:visit_var_insn, Opcodes::ILOAD, :a], (iload :a))
    assert_equal([:visit_var_insn, Opcodes::ASTORE, :a], (astore :a))
  end
  
  def test_ldc
    assert_equal([:visit_ldc_insn, :a], (ldc :a))
  end
  
  def test_method_insns
    assert_equal([:visit_method_insn, Opcodes::INVOKESTATIC, :a, :b, :c], (invokestatic :a, :b, :c))
    assert_equal([:visit_method_insn, Opcodes::INVOKEVIRTUAL, :a, :b, :c], (invokevirtual :a, :b, :c))
    assert_equal([:visit_method_insn, Opcodes::INVOKEINTERFACE, :a, :b, :c], (invokeinterface :a, :b, :c))
    assert_equal([:visit_method_insn, Opcodes::INVOKESPECIAL, :a, :b, :c], (invokespecial :a, :b, :c))
  end
  
  def test_return
    assert_equal([:visit_insn, Opcodes::RETURN], (returnvoid))
  end
  
  def test_bare_insns
    assert_equal([:visit_insn, Opcodes::ARETURN], (areturn))
    assert_equal([:visit_insn, Opcodes::DUP], (dup))
    assert_equal([:visit_insn, Opcodes::SWAP], (swap))
    assert_equal([:visit_insn, Opcodes::POP], (pop))
    assert_equal([:visit_insn, Opcodes::POP2], (pop2))
    assert_equal([:visit_insn, Opcodes::ICONST_0], (iconst_0))
    assert_equal([:visit_insn, Opcodes::ICONST_1], (iconst_1))
    assert_equal([:visit_insn, Opcodes::ICONST_2], (iconst_2))
    assert_equal([:visit_insn, Opcodes::ICONST_3], (iconst_3))
    assert_equal([:visit_insn, Opcodes::LCONST_0], (lconst_0))
    assert_equal([:visit_insn, Opcodes::ISUB], (isub))
    assert_equal([:visit_insn, Opcodes::ACONST_NULL], (aconst_null))
    assert_equal([:visit_insn, Opcodes::NOP], (nop))
    assert_equal([:visit_insn, Opcodes::AALOAD], (aaload))
    assert_equal([:visit_insn, Opcodes::IALOAD], (iaload))
    assert_equal([:visit_insn, Opcodes::BALOAD], (baload))
    assert_equal([:visit_insn, Opcodes::BASTORE], (bastore))
    assert_equal([:visit_insn, Opcodes::DUP_X1], (dup_x1))
    assert_equal([:visit_insn, Opcodes::DUP_X2], (dup_x2))
    assert_equal([:visit_insn, Opcodes::DUP2], (dup2))
    assert_equal([:visit_insn, Opcodes::DUP2_X1], (dup2_x1))
    assert_equal([:visit_insn, Opcodes::DUP2_X2], (dup2_x2))
    assert_equal([:visit_insn, Opcodes::ATHROW], (athrow))
    assert_equal([:visit_insn, Opcodes::ARRAYLENGTH], (arraylength))
    assert_equal([:visit_insn, Opcodes::IADD], (iadd))
    assert_equal([:visit_insn, Opcodes::IINC], (iinc))
  end
  
  def test_type_insns
    assert_equal([:visit_type_insn, Opcodes::NEW, :a], (new :a))
    assert_equal([:visit_type_insn, Opcodes::ANEWARRAY, :a], (anewarray :a))
    assert_equal([:visit_type_insn, Opcodes::NEWARRAY, :a], (newarray :a))
    assert_equal([:visit_type_insn, Opcodes::INSTANCEOF, :a], (instanceof :a))
    assert_equal([:visit_type_insn, Opcodes::CHECKCAST, :a], (checkcast :a))
  end
  
  def test_field_insns
    assert_equal([:visit_field_insn, Opcodes::GETFIELD, :a, :b, :c], (getfield :a, :b, :c))
    assert_equal([:visit_field_insn, Opcodes::PUTFIELD, :a, :b, :c], (putfield :a, :b, :c))
    assert_equal([:visit_field_insn, Opcodes::GETSTATIC, :a, :b, :c], (getstatic :a, :b, :c))
    assert_equal([:visit_field_insn, Opcodes::PUTSTATIC, :a, :b, :c], (putstatic :a, :b, :c))
  end
  
  def test_trycatch
    assert_equal([:visit_try_catch_block, :a, :b, :c, :d], (trycatch :a, :b, :c, :d))
  end
  
  def test_jump_insns
    assert_equal([:visit_jump_insn, Opcodes::GOTO, :a], (goto :a))
    assert_equal([:visit_jump_insn, Opcodes::IFEQ, :a], (ifeq :a))
    assert_equal([:visit_jump_insn, Opcodes::IFNE, :a], (ifne :a))
    assert_equal([:visit_jump_insn, Opcodes::IF_ACMPEQ, :a], (if_acmpeq :a))
    assert_equal([:visit_jump_insn, Opcodes::IF_ACMPNE, :a], (if_acmpne :a))
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPEQ, :a], (if_icmpeq :a))
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPNE, :a], (if_icmpne :a))
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPLT, :a], (if_icmplt :a))
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPGT, :a], (if_icmpgt :a))
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPLE, :a], (if_icmple :a))
    assert_equal([:visit_jump_insn, Opcodes::IF_ICMPGE, :a], (if_icmpge :a))
    assert_equal([:visit_jump_insn, Opcodes::IFNULL, :a], (ifnull :a))
    assert_equal([:visit_jump_insn, Opcodes::IFNONNULL, :a], (ifnonnull :a))
  end
  
  def test_lookup_switch
    assert_equal([:visit_lookup_switch_insn, :a, :b, :c], (lookupswitch :a, :b, :c))
  end
  
  def test_table_switch
    assert_equal([:visit_table_switch_insn, :a, :b, :c, :d], (tableswitch :a, :b, :c, :d))
  end
end