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

  end
end
