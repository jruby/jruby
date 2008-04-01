require 'rbconfig'
require 'test/unit'

class TestRbconfig < Test::Unit::TestCase
  def test_constants
    assert Config
    assert Config::CONFIG
    assert RbConfig
    assert_equal Config, RbConfig
  end

  def test_windows_host_os_name
    name = Config::CONFIG['host_os']
    # Should be "mswin32" on windows and never
    # "Windows NT" or "Windows Vista", etc.
    assert_no_match(/Windows/, name, "Must not contain full Windows name")
  end
end
