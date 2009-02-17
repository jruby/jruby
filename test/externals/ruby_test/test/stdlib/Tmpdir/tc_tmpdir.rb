########################################################################
# tc_tmpdir.rb
#
# Test case for the tmpdir library.
########################################################################
require 'test/unit'
require 'test/helper'
require 'tmpdir'
require 'fileutils'

class TC_Tmpdir_Stdlib < Test::Unit::TestCase
   include Test::Helper

   # Helper method used to unset the environment variables in order to
   # validate the behavior when these cannot be found by the library.
   def remove_env_tmpdirs
      ENV['TMPDIR'] = nil
      ENV['TMP'] = nil
      ENV['TEMP'] = nil
      ENV['USERPROFILE'] = nil
   end

   def setup
      @pwd   = Dir.pwd
      @tmp   = get_temp_path
      @mytmp = File.join(@pwd, 'mytemp')
      @ftmp  = 'tmpdir.tmp'

      touch(@ftmp)
      Dir.mkdir(@mytmp) unless File.exists?(@mytmp)
   end

   def test_tmpdir_basic
      assert_respond_to(Dir, :tmpdir)
      assert_nothing_raised{ Dir.tmpdir }
      assert_equal(true, File.exists?(Dir.tmpdir))
   end

   def test_tmpdir
#      assert_equal(@tmp, Dir.tmpdir)
   end

   # Dir.tmpdir resorts to a specific directory if none of the standard
   # ENV variables can be found.
   #
   def test_tmpdir_env_not_found
      remove_env_tmpdirs
      if WINDOWS
         temp = File.join(get_windows_path, 'temp')
         assert_equal(temp, Dir.tmpdir)
      else
         assert_equal('/tmp', Dir.tmpdir)
      end
   end

   # Dir.tmpdir defaults to '.' (or the WINDOWS directory) if the tmpdir
   # exists but is not a directory.
   #
   def test_tmpdir_is_not_a_file
      ENV['TMPDIR'] = @ftmp
      if WINDOWS
         temp = File.join(get_windows_path, 'temp')
         assert_equal(temp, Dir.tmpdir)
      else
         assert_equal('/tmp', Dir.tmpdir)
      end
   end

   # Dir.tmpdir defaults to '.' (or the WINDOWS directory) if the tmpdir
   # exists but is not writable.
   #--
   # TODO: Fix. The File.chmod doesn't seem to be taking effect on Windows.
   #
   def test_tmpdir_is_not_writable
      remove_env_tmpdirs
      File.chmod(0444, @mytmp)
      ENV['TMPDIR'] = @mytmp

      if WINDOWS
         temp = File.join(get_windows_path, 'temp')
         assert_equal(temp, Dir.tmpdir)
      else
         assert_equal('/tmp', Dir.tmpdir)
      end
   end

   # Dir.tmpdir defaults to your system's tmpdir (always) if the $SAFE
   # level is greater than 0.
   #
#   def test_with_safe_level
#      proc do
#         $SAFE = 1
#         if WINDOWS
#            temp = File.join(get_windows_path, 'temp')
#            assert_equal(temp, Dir.tmpdir)
#         else
#            assert_equal('/tmp', Dir.tmpdir)
#         end
#      end.call
#   end

   def teardown
      FileUtils.rm_rf(@mytmp) if File.exists?(@mytmp)
      File.delete(@ftmp) if File.exists?(@ftmp)
      @pwd   = nil
      @tmp   = nil
      @mytmp = nil
      @ftmp  = nil
   end
end
