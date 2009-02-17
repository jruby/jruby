###############################################################################
# tc_superclass.rb
#
# Test case for the Class#superclass instance method.
###############################################################################
require 'test/unit'

class TC_Class_Superclass_InstanceMethod < Test::Unit::TestCase
   class Superclass
   end

   module SuperModule
   end

   class SubclassOne < Superclass
   end

   class SubclassTwo
      include SuperModule
   end

   class SubclassThree < Superclass
      include SuperModule
   end

   def test_superclass_basic
      assert_respond_to(Superclass, :superclass)
      assert_nothing_raised{ Superclass.superclass }
      assert_kind_of(Object, Superclass.superclass)
   end

   def test_superclass
      assert_equal(Object, Superclass.superclass)
      assert_equal(Superclass, SubclassOne.superclass)
   end

   def test_superclass_with_mixins
      assert_equal(Object, SubclassTwo.superclass)
      assert_equal(Superclass, SubclassThree.superclass)
   end
end
