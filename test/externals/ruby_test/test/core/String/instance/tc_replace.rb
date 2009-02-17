######################################################################
# tc_replace.rb
#
# Test case for the String#replace instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_String_Replace_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @string1 = '<html><b>hello</b></html>'
      @string2 = @string1
   end

   def test_replace_basic
      assert_respond_to(@string1, :replace)
      assert_nothing_raised{ @string1.replace("") }
   end

   def test_replace
      assert_equal("x,y,z", @string1.replace("x,y,z"))
      assert_equal("x,y,z", @string1)
      assert_equal(@string2, @string1)
      assert_equal(@string2.object_id, @string1.object_id)
   end
   
   def test_replace_with_itself
      assert_nothing_raised{ @string1.replace(@string1) }
      assert_equal('<html><b>hello</b></html>', @string1)
      assert_equal(@string1.object_id, @string1.replace(@string1).object_id)
   end

   def test_replace_with_tainted_string
      assert_equal(false, @string1.replace('world').tainted?)
      assert_equal(true, @string1.replace('world'.taint).tainted?)
   end
   
   # String#replace is illegal in $SAFE level >= 4 for untainted strings.
   unless JRUBY
      def test_replace_in_safe_mode
         assert_raise(SecurityError){
            proc do
               $SAFE = 4
               @string1.replace('world')
            end.call
         }
      
         assert_nothing_raised{
            proc do
               $SAFE = 3
               @string1.taint
               @string1.replace('test')
            end.call
         }
      end
   end

   def test_replace_expected_errors
      assert_raise(ArgumentError){ @string1.replace("x","y") }
      assert_raise(TypeError){ @string1.replace(1) }
      assert_raise(TypeError){ @string1.freeze.replace('') }
   end

   def teardown
      @string1 = nil
      @string2 = nil
   end
end
