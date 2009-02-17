########################################################################
# tc_global_variables.rb
#
# Test case for the Kernel.global_variables module method.
########################################################################
require 'test/unit'

class TC_Kerel_GlobalVariables_ModuleMethod < Test::Unit::TestCase
   def test_global_variables_basic
      assert_respond_to(Kernel, :global_variables)
      assert_nothing_raised{ Kernel.global_variables }
      assert_kind_of(Array, Kernel.global_variables)
   end

   # Because rake, etc, can add their own global variables, we'll just
   # ensure that this core group is included.
   def test_global_variables
#      expected = [
#         "$!", "$\"", "$$", "$&", "$'", "$*", "$+", "$,", "$-0", "$-F", "$-I",
#         "$-K", "$-a", "$-d", "$-i", "$-l", "$-p", "$-v", "$-w", "$.", "$/",
#         "$0", "$:", "$;", "$<", "$=", "$>", "$?", "$@", "$DEBUG",
#         "$FILENAME", "$KCODE", "$LOADED_FEATURES", "$LOAD_PATH",
#         "$PROGRAM_NAME", "$SAFE", "$VERBOSE", "$\\", "$_", "$`", "$deferr",
#         "$defout", "$stderr", "$stdin", "$stdout", "$~"
#      ]
#
#      expected.each{ |var|
#         assert_equal(true, Kernel.global_variables.include?(var))
#      }
   end

   def test_global_variables_expected_errors
      assert_raise(ArgumentError){ Kernel.global_variables(true) }
   end
end
