######################################################################
# tc_inherited.rb
#
# Test case for the Class.inherited method.
######################################################################
require 'test/unit'

$subclass = nil

class FooClass
   def self.inherited(sub)
      $subclass = sub.to_s
   end
end

class BarClass < FooClass; end

class TC_Class_Inherited < Test::Unit::TestCase
   def setup
      @foo = FooClass.new
   end

   def test_inherited_basic
      assert_respond_to(FooClass, :inherited)
   end

   def test_inherited_value
      assert_equal("BarClass", $subclass)
   end

   def test_cant_subclass_class
      assert_raise(TypeError){ eval("class Foo < Class; end") }
   end

   def teardown
      @foo = nil
   end
end
