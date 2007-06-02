###############################################
# tc_size.rb
#
# Test suite for the File.size method.
###############################################
require "test/unit"

class TC_File_Size < Test::Unit::TestCase
   def setup
      @zero_file  = "zero.test"  # Size 0
      @small_file = "small.test" # Size > 0 < 2 GB
      @large_file = "large.test" # Size > 2 GB

      @zero_fd  = File.open("zero.test","wb+")
      @small_fd = File.open("small.test", "wb+")
      @large_fd = File.open("large.test", "wb+")

      @small_fd.syswrite("a")
   end

   def test_size
      
   end

   def teardown
      @zero_fd.close
      @small_fd.close
      @large_fd.close

      File.unlink(@zero_file) if File.exists?(@zero_file)
      File.unlink(@small_file) if File.exists?(@small_file)
      File.unlink(@large_file) if File.exists?(@large_file)
   end
end
