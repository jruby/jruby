########################################################################
# tc_isatty.rb
#
# Test case for the IO#isatty instance method and the IO#tty? alias.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Isatty_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @null = null_device
      @file = 'tc_isatty.txt'
      @fh   = File.new(@file, 'wb')
      @nh   = File.new(@null)
      unless WINDOWS
         @tty  = '/dev/tty'
         @th = File.new(@tty) if File.exists?(@tty)
      end
   end

   def test_isatty_basic
      assert_respond_to(@fh, :isatty)
      assert_nothing_raised{ @fh.isatty }
      assert_kind_of(Boolean, @fh.isatty)
   end

   def test_tty_alias_basic
      assert_respond_to(@fh, :isatty)
      assert_nothing_raised{ @fh.isatty }
      assert_kind_of(Boolean, @fh.isatty)
   end

   def test_isatty
      assert_equal(false, @fh.isatty)

      if WINDOWS
         assert_equal(true, @nh.isatty)
      else
         assert_equal(false, @nh.isatty)
         assert_equal(true, @th.isatty) if @th
      end
   end

   # I'm assuming you don't run your test cases via cron...
   def test_isatty_stdout
#      assert_equal(true, STDOUT.isatty)
   end

   def test_isatty_expected_errors
      assert_raise(ArgumentError){ @fh.isatty(1) }
   end

   def teardown
      @fh.close unless @fh.closed?
      @nh.close unless @nh.closed?
      @th.close if @th && !@th.closed? unless WINDOWS

      remove_file(@file)

      @null = nil
      @file = nil
      @tty  = nil
   end
end
