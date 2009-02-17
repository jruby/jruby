######################################################################
# tc_fileno.rb
#
# Test case for the IO#fileno instance method and the IO#to_i alias.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Fileno_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file   = 'fileno_test.txt'
      @handle = File.new(@file, 'w')
   end

   def test_fileno
      assert_respond_to(@handle, :fileno)
      assert_kind_of(Fixnum, @handle.fileno)
      assert_equal(true, @handle.fileno > 2)
   end

   def test_to_i_alias
      assert_respond_to(@handle, :fileno)
      assert_kind_of(Fixnum, @handle.fileno)
      assert_equal(true, @handle.fileno > 2)
   end

   def test_fileno_special_handles
      assert_respond_to(STDIN, :fileno)
      assert_respond_to(STDOUT, :fileno)
      assert_respond_to(STDERR, :fileno)

      assert(STDIN.fileno == 0)
      assert(STDOUT.fileno == 1)
      assert(STDERR.fileno == 2)
   end

   def test_to_i_alias_special_handles
      assert_respond_to(STDIN, :to_i)
      assert_respond_to(STDOUT, :to_i)
      assert_respond_to(STDERR, :to_i)

      assert(STDIN.to_i == 0)
      assert(STDOUT.to_i == 1)
      assert(STDERR.to_i == 2)
   end

   def test_fileno_expected_errors
      assert_raise(ArgumentError){ @handle.fileno(4) }
   end

   def test_to_i_alias_expected_errors
      assert_raise(ArgumentError){ @handle.fileno(4) }
   end

   def teardown
      @handle.close if @handle && !@handle.closed?
      @handle = nil
      remove_file(@file)
   end
end
