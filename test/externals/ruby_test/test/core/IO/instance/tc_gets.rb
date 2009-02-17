###############################################################################
# tc_gets.rb
#
# Test case for the IO#gets instance method.
###############################################################################
require 'test/unit'

class TC_IO_Gets_InstanceMethod < Test::Unit::TestCase
   def setup
      @file   = 'test_gets.txt'
      @handle = File.new(@file, 'wb+')
      @handle.print "hello\nworld\n\nalpha\nbeta\n\ngamma\ndelta"
      @handle.rewind
   end

   def test_gets_basic
      assert_respond_to(@handle, :gets)
      assert_nothing_raised{ @handle.gets }
      assert_kind_of(String, @handle.gets)
   end

   def test_gets
      assert_equal("hello\n", @handle.gets)
      assert_equal("world\n", @handle.gets)
   end

   def test_gets_with_empty_separator
      assert_equal("hello\nworld\n\n", @handle.gets(''))
      assert_equal("alpha\nbeta\n\n", @handle.gets(''))
      assert_equal("gamma\ndelta", @handle.gets(''))
      assert_nil(@handle.gets)
   end

   def test_gets_with_nil_separator
      assert_equal("hello\nworld\n\nalpha\nbeta\n\ngamma\ndelta", @handle.gets(nil))
      assert_nil(@handle.gets)
   end

   def test_gets_with_string_separator
      assert_equal("hello\nworld\n\nalpha", @handle.gets('alpha'))
      assert_equal("\nbeta\n\ngamma\ndelta", @handle.gets('alpha'))
      assert_nil(@handle.gets)
   end

   def test_gets_assigns_to_dollar_underscore
      assert_nothing_raised{ @handle.gets }
      assert_equal("hello\n", $_)
   end

   def test_gets_expected_errors
      assert_raise(TypeError){ @handle.gets(1) }
      assert_raise(ArgumentError){ @handle.gets('', 1) }
   end

   def teardown
      @handle.close unless @handle.closed?
      File.delete(@file) if File.exists?(@file)
      @file   = nil
      @handle = nil
   end
end
