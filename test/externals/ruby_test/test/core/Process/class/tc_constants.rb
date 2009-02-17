########################################################################
# tc_constants.rb
#
# Test case to validate the presence of the various Process constants.
#
# Note that the actual values are not tested because they are set by
# the OS and are not predictable (beyond being Fixnum's).
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Process_Constants < Test::Unit::TestCase
   include Test::Helper
   
   if WINDOWS
      def test_stub
         # Stub to avoid complaints for no tests
      end
   else
      def test_priority_constants
         assert_not_nil(Process::PRIO_PGRP)
         assert_not_nil(Process::PRIO_PROCESS)
         assert_not_nil(Process::PRIO_USER)

         assert_kind_of(Fixnum, Process::PRIO_PGRP)
         assert_kind_of(Fixnum, Process::PRIO_PROCESS)
         assert_kind_of(Fixnum, Process::PRIO_USER)
      end

      def test_child_handling_constants
         assert_not_nil(Process::WNOHANG)
#         assert_not_nil(Process::WUNTRACED)

         assert_kind_of(Fixnum, Process::WNOHANG)
#         assert_kind_of(Fixnum, Process::WUNTRACED)
      end

      if RELEASE >= 5
         def test_rlimit_constants
            assert_not_nil(Process::RLIM_INFINITY)
            assert_not_nil(Process::RLIM_SAVED_MAX)
            assert_not_nil(Process::RLIM_SAVED_CUR)
            assert_not_nil(Process::RLIMIT_CORE)
            assert_not_nil(Process::RLIMIT_CPU)
            assert_not_nil(Process::RLIMIT_DATA)
            assert_not_nil(Process::RLIMIT_FSIZE)
            assert_not_nil(Process::RLIMIT_NOFILE)
            assert_not_nil(Process::RLIMIT_STACK)
            assert_not_nil(Process::RLIMIT_AS)

            if LINUX || BSD
               assert_not_nil(Process::RLIMIT_MEMLOCK)
               assert_not_nil(Process::RLIMIT_NPROC)
               assert_not_nil(Process::RLIMIT_RSS)
            end

            if BSD
               assert_not_nil(Process::RLIMIT_SBSIZE)
            end
         end
      end
   end
end
