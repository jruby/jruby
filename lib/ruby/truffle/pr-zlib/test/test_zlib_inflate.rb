########################################################################
# test_zlib_inflate.rb
#
# Test case for the Zlib::Inflate class.
########################################################################
require 'pr/zlib'
require 'test-unit'

class TC_Zlib_Inflate < Test::Unit::TestCase
   def self.startup
   end

   def setup
      @inflate = Zlib::Inflate.new
   end

   def test_inflate_run_singleton_basic
      assert_respond_to(Zlib::Inflate, :inflate_run)
   end

   def test_inflate_singleton_basic
      assert_respond_to(Zlib::Inflate, :inflate)
   end

   #def test_initialize_copy_basic
      #assert_respond_to(@inflate, :initialize_copy)
   #end

   def test_inflate_basic
      assert_respond_to(@inflate, :inflate)
   end

   def test_append_basic
      assert_respond_to(@inflate, :<<)
   end

   def test_sync_basic
      assert_respond_to(@inflate, :sync)
   end

   def test_is_sync_point_basic
      assert_respond_to(@inflate, :sync_point?)
   end

   def test_set_dictionary_basic
      assert_respond_to(@inflate, :set_dictionary)
   end

   def teardown
      @inflate = nil
   end

   def self.shutdown
   end
end
