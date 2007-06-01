##########################################################################
# tc_chown.rb
#
# Test suite for the File#chown? instance method. These tests are skipped
# on MS Windows.
#
# On UNIX systems, I require the 'etc' package in order to get a user and
# group that I can use.
#
# This test case is somewhat complicated by the fact that the
# restrictions on File#chown vary from platform to platform, with
# some platforms allowing configuration via the _POSIX_CHOWN_RESTRICTED
# constant in unistd.h.
#
# For now, I'll follow the typical restriction found on most systems,
# i.e. that you must be root to successfully chown a file. Thus, it is
# best if you run this test case as root. If not, some tests will be
# skipped.
##########################################################################
require 'test/unit'
require 'test/helper'
require 'etc' rescue nil

class TC_File_Chown_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   if WINDOWS
      def test_stub
         # Stub to keep Test::Unit from whining about no defined tests.
      end
   else
      def setup
         @name1 = "temp1.txt"
         @name2 = "temp2.txt"

         system("touch #{@name1}")
         system("touch #{@name2}")

         @file1 = File.open(@name1)
         @file2 = File.open(@name2)
         @root  = Process.euid == 0
         @uid   = Etc.getpwnam('nobody').uid
         @gid   = Etc.getgrnam('nobody').gid
      end

      def test_chown_basic
         assert_respond_to(@file1, :chown)
         assert_nothing_raised{ @file2.chown(-1, -1) }
      end

      def test_chown
         assert_equal(0, @file1.chown(-1, -1))
         if @root
            assert_equal(0, @file1.chown(@uid, -1, @file1))
            assert_equal(0, @file1.chown(-1, @gid))
            assert_equal(0, @file1.chown(@uid, @gid))
         end
      end

      def test_chown_expected_errors
         assert_raises(ArgumentError){ @file1.chown(-1) }
         assert_raises(TypeError){ @file1.chown('bogus', -1) }
         assert_raises(TypeError){ @file1.chown(-1, 'bogus') }
      end

      def teardown
         @file1.close
         @file2.close

         File.delete(@name1) if File.exists?(@name1)
         File.delete(@name2) if File.exists?(@name2)

         @file = nil
         @root = nil
         @uid  = nil
         @gid  = nil
      end
   end
end
