########################################################################
# tc_flock.rb
#
# Test case for the File#flock instance method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Flock_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @name = 'flock_instance_test.txt'
      @file = File.open(@name, 'w+')
   end

   def test_flock_basic
      assert_respond_to(@file, :flock)
   end

   # The second call causes Ruby to block on Windows. See RubyForge bug #18213.
   def test_flock_exclusive
      assert_nothing_raised{ @file.flock(File::LOCK_EX) }
      assert_equal(0, @file.flock(File::LOCK_EX)) unless WINDOWS
   end

   def test_flock_shared
      assert_nothing_raised{ @file.flock(File::LOCK_SH) }
      assert_equal(0, @file.flock(File::LOCK_SH))
   end

   def test_flock_unlock
      assert_nothing_raised{ @file.flock(File::LOCK_UN) }
#      assert_equal(0, @file.flock(File::LOCK_UN))
   end

   # TODO: Create separate tests for an expected 0 and an expected false.
   def test_flock_nonblocking
      assert_nothing_raised{ @file.flock(File::LOCK_EX | File::LOCK_NB) }
      assert_equal(true,
         [0, false].include?(@file.flock(File::LOCK_EX | File::LOCK_NB))
      )
   end

   def teardown
      @file.close unless @file.closed?
      File.delete(@name) if File.exists?(@name)
      @name = nil
   end
end
