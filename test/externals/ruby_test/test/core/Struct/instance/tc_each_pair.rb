######################################################################
# tc_each_pair.rb
#
# Test case for the Struct#each_pair instance method.
######################################################################
require 'test/unit'

class TC_Struct_EachPair_InstanceMethod < Test::Unit::TestCase
   def setup
      Struct.new('EPair', :name, :age, :tall) unless defined? Struct::EPair
      @struct = Struct::EPair.new('Dan', 37, true)
      @array  = []
   end

   def test_each_pair_basic
      assert_respond_to(@struct, :each_pair)
      assert_nothing_raised{ @struct.each_pair{} }
   end

   def test_each_pair
      assert_nothing_raised{ @struct.each_pair{ |k,v| @array << [k,v] } }
      assert_equal([[:name,'Dan'], [:age,37], [:tall, true]], @array)
   end

   def teardown
      Struct.send(:remove_const, 'EPair') if defined? Struct::EPair
      @struct = nil
      @array  = nil
   end
end
