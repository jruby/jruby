######################################################################
# tc_popen.rb
#
# Test case for the IO.popen class method. Some tests will be
# skipped on MS Windows.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Popen_ClassMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @cmd = WINDOWS ? 'date /t' : 'date'
      @str = nil
      @io  = nil
   end

   def test_popen_basic
      assert_respond_to(IO, :popen)
      assert_kind_of(IO, @io = IO.popen(@cmd))
   end

   # Call popen, read from the IO object, check its type and length, try
   # reading again, and check the length of the returned object again.
   #
   def test_popen
      assert_nothing_raised{ @io = IO.popen(@cmd) }
      assert_nothing_raised{ @str = @io.read }
      assert_kind_of(String, @str)
      assert_equal(true, @str.length > 0)
      assert_nothing_raised{ @str = @io.read }
      assert_equal(true, @str.length == 0)
   end

   def teardown
      sleep 0.1 if WINDOWS # To silence warnings
      @io.close if @io rescue nil
      @cmd = nil
   end
end
