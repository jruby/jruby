#####################################################################
# tc_abort.rb
#
# Test case for the Process.abort module method. I have no idea
# how to properly test this without a fork() function on MS Windows.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_Process_Abort_ModuleMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @stderr = STDERR.clone
      @file   = File.join(Dir.pwd, 'tc_abort.txt')
      @fh     = File.open(@file, "w")
      STDERR.reopen(@fh)
   end

   def test_abort_basic
      assert_respond_to(Process, :abort)
   end

   unless WINDOWS
#      def test_abort
#         fork{ Process.abort }
#         pid, status = Process.wait2
#         assert_equal(1, status.exitstatus)
#      end

#      def test_abort_with_error_message
#         fork{ Process.abort("hello world") }
#         pid, status = Process.wait2
#
#         assert_equal(1, status.exitstatus)
#         assert_equal("hello world", IO.read(@file).chomp)
#      end
   end
      
   def teardown
      @fh.close if @fh && !@fh.closed?
      STDERR.reopen(@stderr)
      File.delete(@file) if File.exists?(@file)  
   end
end
