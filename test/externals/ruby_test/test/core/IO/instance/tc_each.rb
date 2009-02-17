###############################################################################
# tc_each.rb
#
# Test case for the IO#each instance method and the IO#each_line alias.
###############################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Each_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file   = 'test_io_each.txt'
      @handle = File.new(@file, 'w+')
      @array  = []
      @handle << "Hello\n" << "World\n"
      @handle.rewind
   end

   def test_each_basic
      assert_respond_to(@handle, :each)
      assert_nothing_raised{ @handle.each{} }
   end
   
   def test_each_line_basic
      assert_respond_to(@handle, :each_line)
      assert_nothing_raised{ @handle.each_line{} }   
   end

   def test_each_no_separator
      assert_nothing_raised{ @handle.each{ |line| @array << line } }
      assert_equal(["Hello\n", "World\n"], @array)
   end
   
   def test_each_line_no_separator
      assert_nothing_raised{ @handle.each_line{ |line| @array << line } }
      assert_equal(["Hello\n", "World\n"], @array)
   end

   def test_each_with_separator
      assert_nothing_raised{ @handle.each('l'){ |line| @array << line } }
      assert_equal(["Hel", "l", "o\nWorl", "d\n"], @array)
   end
   
   def test_each_line_with_separator
      assert_nothing_raised{ @handle.each_line('l'){ |line| @array << line } }
      assert_equal(["Hel", "l", "o\nWorl", "d\n"], @array)
   end

   def test_each_with_empty_separator
      assert_nothing_raised{ @handle.each(''){ |line| @array << line } }
      assert_equal(["Hello\nWorld\n"], @array)
   end
   
   def test_each_line_with_empty_separator
      assert_nothing_raised{ @handle.each_line(''){ |line| @array << line } }
      assert_equal(["Hello\nWorld\n"], @array)
   end

   def test_each_expected_errors
      assert_raises(ArgumentError){ @handle.each('x', 2){} }
      # assert_raises(LocalJumpError){ @handle.each } # A block isn't required?
   end
   
   def test_each_line_expected_errors
      assert_raises(ArgumentError){ @handle.each_line('x', 2){} }
      # assert_raises(LocalJumpError){ @handle.each } # A block isn't required?
   end

   def teardown
      @handle.close if @handle && !@handle.closed?
      remove_file(@file)
      @array  = nil
      @handle = nil
      @file   = nil
   end
end
