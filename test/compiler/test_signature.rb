require 'test/unit'
require 'compiler/signature'

class TestSignature < Test::Unit::TestCase
  include Java
  import java.util.ArrayList
  import java.lang.String
  import java.lang.Byte
  import java.lang.Short
  import java.lang.Character
  import java.lang.Boolean
  import java.lang.Integer
  import java.lang.Long
  import java.lang.Float
  import java.lang.Double
  import java.lang.Void

  Signature = Compiler::Signature
  
  def test_classname
    assert_equal("java.util.ArrayList", Signature.classname("java/util/ArrayList"))
    assert_equal("java.util.ArrayList", Signature.c("java/util/ArrayList"))
  end
  
  def test_path
    assert_equal("java/util/ArrayList", Signature.path(ArrayList))
    assert_equal("java/util/ArrayList", Signature.p(ArrayList))
  end
  
  def test_class_id
    assert_equal("Ljava/util/ArrayList;", Signature.class_id(ArrayList))
    assert_equal("Ljava/util/ArrayList;", Signature.ci(ArrayList))
    assert_equal("B", Signature.class_id(Byte::TYPE))
    assert_equal("Z", Signature.class_id(Boolean::TYPE))
    assert_equal("S", Signature.class_id(Short::TYPE))
    assert_equal("C", Signature.class_id(Character::TYPE))
    assert_equal("I", Signature.class_id(Integer::TYPE))
    assert_equal("J", Signature.class_id(Long::TYPE))
    assert_equal("F", Signature.class_id(Float::TYPE))
    assert_equal("D", Signature.class_id(Double::TYPE))
    assert_equal("V", Signature.class_id(Void::TYPE))
  end
  
  def test_signature
    assert_equal("(Ljava/util/ArrayList;)V", Signature.signature(Void::TYPE, ArrayList))
    assert_equal("(Ljava/util/ArrayList;)V", Signature.sig(Void::TYPE, ArrayList))
    assert_equal("([Ljava/lang/String;)V", Signature.signature(Void::TYPE, String[]))
    assert_equal("(BZSCIJFDLjava/util/ArrayList;)V",
      Signature.signature(
        Void::TYPE,
        Byte::TYPE,
        Boolean::TYPE,
        Short::TYPE,
        Character::TYPE,
        Integer::TYPE,
        Long::TYPE,
        Float::TYPE,
        Double::TYPE,
        ArrayList))
  end
end