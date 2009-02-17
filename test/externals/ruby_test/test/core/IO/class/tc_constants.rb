###############################################################################
# tc_constants.rb
#
# Test case to verify the existence and/or value of constants associated
# with the IO class. There may be some overlap between these tests and the
# equivalent File class tests.
#
# I also verify the existence of some toplevel constants that are defined
# in the io.c file.
###############################################################################
require 'test/unit'

class TC_IO_Constants < Test::Unit::TestCase
   def test_seek_constants
      assert_not_nil(IO::SEEK_SET)
      assert_not_nil(IO::SEEK_CUR)
      assert_not_nil(IO::SEEK_END)
   end

   def test_std_constants
      assert_not_nil(STDIN)
      assert_not_nil(STDOUT)
      assert_not_nil(STDERR)
      assert_not_nil(ARGF)
   end
end
