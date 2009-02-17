######################################################################
# tc_ftype.rb
#
# Test case for the File.ftype class method.
#
# TODO: Add tests for 'socket' and 'unknown', and better tests for
# MS Windows.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Ftype_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file = __FILE__
      @dir  = Dir.pwd
      @fifo = nil

      if WINDOWS
         @block_dev = "NUL"
         @fifo  = nil
         @block = nil
         @link  = nil
         @char  = nil
      else
         @char = Pathname.new('/dev/null').realpath
         @fifo = "test_fifo"

         system("mkfifo #{@fifo}")

         if File.exists?("/dev/fd0")
            @block = Pathname.new("/dev/fd0").realpath
            @link  = "/dev/fd0" if File.symlink?("/dev/fd0")
         elsif File.exists?("/dev/diskette")
            @block = Pathname.new("/dev/diskette").realpath
            @link  = "/dev/diskette" if File.symlink?("/dev/diskette")
         elsif File.exists?("/dev/cdrom")
            @block = Pathname.new("/dev/cdrom").realpath
            @link  = "/dev/cdrom" if File.symlink?("/dev/cdrom")
         elsif File.exists?("/dev/sr0") # CDROM
            @block = Pathname.new("/dev/sr0").realpath
            @link  = "/dev/sr0" if File.symlink?("/dev/sr0") 
         elsif File.exists?("/dev/disk0")
            @block = "/dev/disk0"
            @link  = "/tmp"
         else
            @block = nil
            @link  = nil
         end
      end
   end

   def test_ftype_basic
      assert_respond_to(File, :ftype)
      assert_nothing_raised{ File.ftype(@file) } 
      assert_kind_of(String, File.ftype(@file))
   end

   def test_ftype_file
      assert_equal('file', File.ftype(@file))
   end

   def test_ftype_directory
      assert_equal('directory', File.ftype(@dir))
   end

   # TODO: Find appropriate tests for MS Windows
   unless WINDOWS
      def test_ftype_char
         assert_equal('characterSpecial', File.ftype(@char))
      end

      def test_ftype_block
         assert_equal('blockSpecial', File.ftype(@block), "BLOCK WAS: #{@block}")
      end

      def test_ftype_link
         assert_equal('link', File.ftype(@link))
      end

      def test_ftype_fifo
         assert_equal('fifo', File.ftype(@fifo))
      end
   end

   def test_ftype_expected_errors
      assert_raises(ArgumentError){ File.ftype }
      assert_raises(Errno::ENOENT){ File.ftype('bogus') }
   end

   def teardown
      remove_file(@fifo)
      
      @file   = nil
      @dir    = nil
      @char   = nil
      @block  = nil
      @fifo   = nil
      @link   = nil
      @socket = nil
   end
end
