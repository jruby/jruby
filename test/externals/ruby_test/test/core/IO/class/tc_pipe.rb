######################################################################
# tc_pipe.rb
#
# Test case for the IO.pipe class method. These tests are skipped
# on MS Windows (via the Rakefile).
######################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Pipe_ClassMethod < Test::Unit::TestCase
   def setup
      @read  = nil
      @write = nil
   end

   def test_pipe_basic
      assert_respond_to(IO, :pipe)
      assert_kind_of(Array, IO.pipe)
   end

   def test_pipe
      assert_nothing_raised{ @read, @write = IO.pipe }
      assert_kind_of(IO, @read)
      assert_kind_of(IO, @write)
   end

   def test_pipe_read_and_write
      assert_nothing_raised{ @read, @write = IO.pipe }
      assert_nothing_raised{ @write.print "hello" }
      assert_nothing_raised{ @write.close }
      assert_equal("hello", @read.read)
      assert_nothing_raised{ @read.close }
   end

   def test_pipe_expected_errors
      assert_raise(ArgumentError){ IO.pipe(2) }
   end

   def teardown
      @read.close if @read && !@read.closed? 
      @write.close if @write && !@write.closed?
   end
end
