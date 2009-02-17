########################################################################
# tc_lineno.rb
#
# Test case for the IO#lineno and IO#lineno= instance methods.
########################################################################
require 'test/unit'

class TC_IO_Lineno_InstanceMethod < Test::Unit::TestCase
   def setup
      @file   = 'test_gets.txt'
      @handle = File.new(@file, 'wb+')
      @handle.print "hello\nworld\n\nalpha\nbeta\n\ngamma\ndelta"
      @handle.rewind
   end

   def test_lineno_basic
      assert_respond_to(@handle, :lineno)
      assert_nothing_raised{ @handle.lineno }
   end

   def test_lineno_one_line_read
      assert_equal(0, @handle.lineno)
      assert_nothing_raised{ @handle.gets }
      assert_equal(1, @handle.lineno)
   end

   def test_lineno_multi_line_read
      assert_equal(0, @handle.lineno)
      assert_nothing_raised{ 3.times{ @handle.gets } }
      assert_equal(3, @handle.lineno)
   end

   def test_lineno_read_past_end
      assert_equal(0, @handle.lineno)
      assert_nothing_raised{ 20.times{ @handle.gets } }
      assert_equal(8, @handle.lineno)
   end

   def test_lineno_nil_separator
      assert_equal(0, @handle.lineno)
      assert_nothing_raised{ @handle.gets(nil) }
      assert_equal(1, @handle.lineno)
   end

   def test_lineno_empty_separator
      assert_equal(0, @handle.lineno)
      assert_nothing_raised{ 20.times{ @handle.gets('') } }
      assert_equal(3, @handle.lineno)
   end

   def test_lineno_set_basic
      assert_respond_to(@handle, :lineno=)
      assert_nothing_raised{ @handle.lineno = 0 }
   end

   def test_lineno_set
      assert_nothing_raised{ @handle.lineno = 100 }
      assert_nothing_raised{ @handle.gets }
      assert_equal(101, @handle.lineno)
   end

   # Strange
   def test_lineno_set_negative
      assert_nothing_raised{ @handle.lineno = -100 }
      assert_nothing_raised{ @handle.gets }
      assert_equal(-99, @handle.lineno)
   end

   def test_lineno_sets_lineno_global
      assert_nothing_raised{ @handle.lineno = 100 }
      assert_nothing_raised{ @handle.gets }
      assert_equal(101, $.)
   end

   def test_lineno_expected_errors
      assert_raise(ArgumentError){ @handle.lineno(0) }
   end

   def test_lineno_set_expected_errors
      assert_raise(ArgumentError){ @handle.send(:lineno=) }
      assert_raise(ArgumentError){ @handle.send(:lineno=, 1, 2) }
   end

   def teardown
      @handle.close unless @handle.closed?
      File.delete(@file) if File.exists?(@file)
      @file   = nil
      @handle = nil
   end
end
