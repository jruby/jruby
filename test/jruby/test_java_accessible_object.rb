require 'test/unit'
require 'java'

# JavaAccessibleObjects are used (in the guise of constructors and methods)
# in javasupport's JavaUtilities.matching_method.
# 
# A cache is used to where the keys are arrays of JavaAccessibleObject.
# Therefore, it's important that they work as hash keys with hash and
# eql?.
class TestJavaAccessibleObject < Test::Unit::TestCase

  def setup
    @c1 = java.lang.Object.java_class.constructors.first
    @c2 = java.lang.Object.java_class.constructors.first
  end
  
  def test_hash
    assert_equal @c1.hash, @c2.hash
  end

  def test_equal_equal
    assert @c1 == @c2
  end

  def test_eql
    assert @c1.eql?(@c2)
  end
end
