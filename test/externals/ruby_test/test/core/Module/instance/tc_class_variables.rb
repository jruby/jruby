########################################################################
# tc_class_variables.rb
#
# Test case for the Module#class_variables instance method.
########################################################################
require 'test/unit'

module CV_Mod_A
   @@var1 = 1
end

class CV_Class_A
   include CV_Mod_A
   @@var2 = 2
end

# Overwrite the class variable
class CV_Class_B
   include CV_Mod_A
   @@var1 = 2
end

# Remove the class variable
class CV_Class_C
   @@var3 = 3
   @@var4 = 4
   remove_class_variable(:@@var3)
end

class TC_Module_ClassVariables_InstanceMethod < Test::Unit::TestCase
   def setup
      @obj_c = CV_Class_C.new
   end

   def test_class_variables_basic
      assert_respond_to(CV_Mod_A, :class_variables)
      assert_nothing_raised{ CV_Mod_A.class_variables }
      assert_kind_of(Array, CV_Mod_A.class_variables)
   end

   def test_class_variables
      assert_equal(['@@var1'], CV_Mod_A.class_variables)
#      assert_equal(['@@var2', '@@var1'], CV_Class_A.class_variables)
      assert_equal(['@@var1'], CV_Class_B.class_variables)
   end

   def test_class_variables_after_removal
      assert_equal(['@@var4'], CV_Class_C.class_variables)
   end

   def test_class_variables_expected_errors
      assert_raise(ArgumentError){ CV_Class_A.class_variables(true) }
   end

   def teardown
      @obj_c = nil
   end
end
