########################################################################
# tc_define_finalizer.rb
#
# Test case for the ObjectSpace.define_finalizer.
#
# TODO: Figure out a way to fire off the finalizer and test the result
# without exiting the program.
########################################################################
require 'test/unit'

class TC_ObjectSpace_DefineFinalizer_ClassMethod < Test::Unit::TestCase
   def setup
      @array  = [1,2,3]
      @hash   = {1,2,3,4}
      @string = 'hello'
      @final  = nil
      @proc   = proc{ @final = 'test' }
   end

   def test_define_finalizer_basic
      assert_respond_to(ObjectSpace, :define_finalizer)
   end

   def test_define_finalizer_with_proc
      assert_nothing_raised{ ObjectSpace.define_finalizer(@array, @proc) }
      assert_nil(@final)
   end

   def test_define_finalizer_with_block
      assert_nothing_raised{
         ObjectSpace.define_finalizer(@hash){ @final = 'test2' }
      }
      assert_nil(@final)
   end

   # What should happen here?
   def test_define_finalizer_with_block_and_proc
      assert_nothing_raised{
         ObjectSpace.define_finalizer(@string, @proc){ @final = 'test3' }
      }
      assert_nil(@final)
   end

   def test_define_finalizer_expected_errors
      assert_raise(ArgumentError){ ObjectSpace.define_finalizer }
      assert_raise(ArgumentError){ ObjectSpace.define_finalizer(@array) }
      assert_raise(ArgumentError){ ObjectSpace.define_finalizer(@array, 7) }
      assert_raise(ArgumentError){ ObjectSpace.define_finalizer(@array, @proc, 7) }
   end

   def teardown
      @array = nil
      @hash  = nil
      @final = nil
      @proc  = nil
   end
end
