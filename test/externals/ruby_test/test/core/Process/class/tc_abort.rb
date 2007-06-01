#####################################################################
# tc_abort.rb
#
# Test case for the Process.abort module method.
#####################################################################
require 'test/unit'

class TC_Process_Abort_ModuleMethod < Test::Unit::TestCase
   def setup
      @stderr = STDERR.clone
      @file   = "temp.txt"
      @fh     = File.open(@file, "w")
      STDERR.reopen(@file)
   end

   def test_abort_basic
      assert_respond_to(Process, :abort)
   end

   def test_abort
      fork{ Process.abort }
      pid, status = Process.wait2
      assert_equal(1, status.exitstatus)
   end

   def test_abort_with_error_message
      fork{ Process.abort("hello world") }
      pid, status = Process.wait2

      assert_equal(1, status.exitstatus)
      assert_equal("hello world", IO.read(@file).chomp)
   end

   def teardown
      STDERR.reopen(@stderr)
      File.delete(@file) if File.exists?(@file)
      @fh.close
   end
end
