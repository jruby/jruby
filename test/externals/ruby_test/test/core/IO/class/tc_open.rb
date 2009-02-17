######################################################################
# tc_open.rb
#
# Test case for the IO.open class method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_IO_Open_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @stream = nil
      @file   = File.join(Dir.pwd, 'tc_open.txt')
      @fh     = File.open(@file, 'w')
      @fileno = @fh.fileno
   end

   def test_open_basic
      assert_respond_to(IO, :open)
   end

   def test_open
      assert_nothing_raised{ @stream = IO.open(2) }
      assert_kind_of(IO, @stream)
   end

   def test_open_with_block
      assert_nothing_raised{ IO.open(@fh.fileno){ |io| @fileno = io.fileno } }
      assert_raise_kind_of(SystemCallError){ IO.open(@fileno) } # already closed
   end

   def test_open_with_modestring
      assert_nothing_raised{ @stream = IO.open(2, 'w') }
      assert_kind_of(IO, @stream)
   end

   def test_open_with_modestring_with_block
      assert_nothing_raised{ IO.open(@fh.fileno, 'w'){ |io| @fileno = io.fileno } }
      assert_raise_kind_of(SystemCallError){ IO.open(@fileno) } # already closed
   end

   def test_open_expected_errors
      assert_raise(ArgumentError){ IO.open }
      assert_raise(ArgumentError){ IO.open(2, 'w', 2) }
      assert_raise(TypeError){ IO.open("test", 'w') }
      assert_raise_kind_of(SystemCallError){ IO.open(999) }
   end

   def teardown
      @fh.close rescue nil
      remove_file(@file)
      @file   = nil
      @fh     = nil
      @stream = nil
   end
end
