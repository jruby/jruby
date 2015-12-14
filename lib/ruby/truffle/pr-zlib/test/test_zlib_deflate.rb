########################################################################
# test_zlib_deflate.rb
#
# Test case for the Zlib::Deflate class.
########################################################################
require 'pr/zlib'
require 'test-unit'

class TC_Zlib_Deflate < Test::Unit::TestCase
   def self.startup
   end

   def setup
      @deflate = Zlib::Deflate.new
   end

   def test_deflate_run_singleton_basic
      assert_respond_to(Zlib::Deflate, :deflate_run)
   end

   def test_deflate_singleton_basic
      assert_respond_to(Zlib::Deflate, :deflate)
   end

   #def test_initialize_copy_basic
      #assert_respond_to(@deflate, :initialize_copy)
   #end

   def test_deflate_basic
      assert_respond_to(@deflate, :deflate)
   end

   def test_append_basic
      assert_respond_to(@deflate, :<<)
   end

   def test_flush_basic
      assert_respond_to(@deflate, :flush)
   end

   def test_params_basic
      assert_respond_to(@deflate, :params)
   end

   def test_set_dictionary_basic
      assert_respond_to(@deflate, :set_dictionary)
   end

   def teardown
      @deflate = nil
   end

   def self.shutdown
   end
end
