#######################################################################
# tc_struct_tms.rb
#
# Test case for the special Struct::Tms structure. Although this is
# technically a "core" class, it's really just a special type of
# Struct returned by the Process.times method.
########################################################################
require 'test/unit'

class TC_StructTms < Test::Unit::TestCase
   def setup
      @times = Process.times
   end
   
   def test_struct_tms_basic
      assert_kind_of(Struct::Tms, @times)
      assert_respond_to(@times, :utime)
      assert_respond_to(@times, :stime)
      assert_respond_to(@times, :cutime)
      assert_respond_to(@times, :cstime)
   end
   
   def test_struct_tms_member_values
      assert_kind_of(Float, @times.utime)
      assert_kind_of(Float, @times.stime)
      assert_kind_of(Float, @times.cutime)
      assert_kind_of(Float, @times.cstime)
   end
   
   def teardown
      @times = nil
   end
end