########################################################################
# tc_pid.rb
#
# Test case for the IO#pid instance method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Pid_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @cmd  = WINDOWS ? 'date /t' : 'date'      
      @io   = IO.popen(@cmd)
      @file = 'tc_pid.txt'
      @fh   = File.open(@file, 'w')
   end

   def test_pid_basic
      assert_respond_to(@io, :pid)
      assert_kind_of(Integer, @io.pid)
   end

   def test_pid
      assert_equal(true, @io.pid > 0)
   end

   def test_pid_without_popen
      assert_respond_to(@fh, :pid)
      assert_nil(@fh.pid)
   end

   def test_pid_expected_errors
      assert_raise(ArgumentError){ @io.pid(1) }
   end

   def teardown
      sleep 0.1 if WINDOWS # To silence annoying warnings
      @io.close unless @io.closed?
      @fh.close unless @fh.closed?
      remove_file(@file)
      @io   = nil
      @cmd  = nil
      @fh   = nil
      @file = nil
   end
end
