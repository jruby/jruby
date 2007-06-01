######################################################################
# tc_new.rb
#
# Test case for the Module.new module method.
######################################################################
require 'test/unit'

class TC_Module_New_ModuleMethod < Test::Unit::TestCase
   def setup
      @module = nil
      @string = "test string"
   end

   def test_new_basic
      assert_respond_to(Module, :new)
      assert_nothing_raised{ Module.new }
      assert_kind_of(Module, Module.new)
   end

   def test_new
      assert_nothing_raised{
         @module = Module.new do
            def method_one
               "hello"
            end
            def self.method_two
               "world"
            end
         end
      }

      assert_nothing_raised{ @string.extend(@module) }
      assert_equal("hello", @string.method_one)
      assert_raises(NoMethodError){ @string.method_two }
   end


   def teardown
      @module = nil
      @string = nil
   end
end
