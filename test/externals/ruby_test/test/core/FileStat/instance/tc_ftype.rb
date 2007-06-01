######################################################################
# tc_ftype.rb
#
# Test case for the FileStat#ftype? instance method.
######################################################################
require 'test/unit'

class TC_FileStat_FType_Instance < Test::Unit::TestCase
   def setup
      @stat = File::Stat.new(__FILE__)
      @valid = %w/file directory characterSpecial blockSpecial fifo link socket unknown/
   end

   def test_ftype_basic
      assert_respond_to(@stat, :ftype)
      assert_kind_of(String, @stat.ftype)
   end

   def test_ftype
      assert_equal(true, @valid.include?(@stat.ftype))
      assert_equal('file', @stat.ftype)
      assert_equal('directory', File::Stat.new(Dir.pwd).ftype)
   end

   def test_ftype_expected_errors
      assert_raises(ArgumentError){ @stat.ftype(1) }
   end

   def teardown
      @stat = nil
   end
end
