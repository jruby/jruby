require 'test/unit'
require 'test/test_helper'

class TestWin32 < Test::Unit::TestCase
  include TestHelper
  if (WINDOWS)

    def test_win32_registry
      require 'win32/registry'
        Win32::Registry::HKEY_CURRENT_USER.open('Environment', Win32::Registry::KEY_ALL_ACCESS) do |reg|
          # we assume that HKEY_CURRENT_USER\\Environment\\TMP is present in registry
          assert_not_nil reg['TMP']
      end
    end

    def test_win32_resolv
      require 'win32/resolv'
      assert_not_nil Win32::Resolv.get_hosts_path
    end

    # JRUBY-3480
    def test_resolv
      require 'resolv'
      assert_match(/^[\d\.:]+$/, Resolv.getaddress("www.google.com"))
    end

    # JRUBY-4313
    def test_syslog
      assert_raise(LoadError) do
        require 'syslog'
      end
    end

  else
    # make testrunner happy
    def test_noop
    end
  end
end
