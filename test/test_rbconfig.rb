require 'rbconfig'
require 'test/unit'

class TestRbconfig < Test::Unit::TestCase
  def test_constants
    assert Config
    assert RbConfig::CONFIG
    assert RbConfig
    assert_equal Config, RbConfig
  end

  def test_windows_os_names
    name = RbConfig::CONFIG['host_os']
    name1 = RbConfig::CONFIG['target_os']

    assert_not_nil(name)
    assert_not_nil(name1)

    # Should be "mswin32" on windows and never
    # "Windows NT" or "Windows Vista", etc.
    assert_no_match(/Windows/, name, "Must not contain full Windows name")
    assert_no_match(/Windows/, name1, "Must not contain full Windows name")
  end
end
