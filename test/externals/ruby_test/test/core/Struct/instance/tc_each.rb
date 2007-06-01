######################################################################
# tc_each.rb
#
# Test case for the Struct#each instance method.
######################################################################
require 'test/unit'

class TC_Struct_Each_InstanceMethod < Test::Unit::TestCase
   def setup
      Struct.new('Each', :name, :age, :tall) unless defined? Struct::Each
      @struct = Struct::Each.new('Dan', 37, true)
      @array  = []
   end

   def test_each_basic
      assert_respond_to(@struct, :each)
      assert_nothing_raised{ @struct.each{} }
   end

   def test_each
      assert_nothing_raised{ @struct.each{ |s| @array << s } }
      assert_equal(['Dan', 37, true], @array)
   end

   def teardown
      Struct.send(:remove_const, 'Each') if defined? Struct::Each
      @struct = nil
      @array  = nil
   end
end
