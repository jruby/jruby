##########################################################################
# tc_lchown.rb
#
# Test suite for the File.lchown? class method. These tests are mostly
# skipped on MS Windows, Solaris and OS X. 
#
# See the notes in tc_chown.rb for some issues that apply to this test
# case as well.
#
# TODO: Validate that these tests work on Linux, FreeBSD, etc.
##########################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Lchown_ClassMethod < Test::Unit::TestCase
   include Test::Helper
   
   if WINDOWS || OSX || SOLARIS
      def test_stub
         # Stub to keep Test::Unit from whining about no defined tests.
      end
   else
      def setup
         @file1 = "temp1.txt"
         @file2 = "temp2.txt"
         @root  = Process.euid == 0
         @uid   = Etc.getpwnam('nobody').uid
         @gid   = Etc.getgrnam('nobody').gid

         touch(@file1)
         touch(@file2)
      end

      def test_lchown_basic
         assert_respond_to(File, :lchown)
         assert_nothing_raised{ File.lchown(-1, -1, @file1) }
         assert_nothing_raised{ File.lchown(-1, -1) } # No files is ok (?)
      end

      def test_lchown
         assert_equal(1, File.lchown(-1, -1, @file1))
         if @root
            assert_equal(1, File.lchown(@uid, -1, @file1))
            assert_equal(1, File.lchown(-1, @gid, @file1))
            assert_equal(1, File.lchown(@uid, @gid, @file1))
            assert_equal(2, File.lchown(@uid, @gid, @file1, @file2))
         end
      end

      def test_lchown_edge_cases
         assert_equal(0, File.lchown(-1, -1)) # Odd
      end

      def test_lchown_expected_errors
         assert_raises(ArgumentError){ File.lchown(-1) }
         assert_raises(TypeError){ File.lchown('bogus', -1) }
         assert_raises(TypeError){ File.lchown(-1, 'bogus') }
      end

      def teardown
         File.delete(@file1) if File.exists?(@file1)
         File.delete(@file2) if File.exists?(@file2)

         @file = nil
         @root = nil
         @uid  = nil
         @gid  = nil
      end
   end
end
