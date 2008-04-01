require 'test/unit'
require 'rbconfig'
require 'etc'

class TestEtc < Test::Unit::TestCase
  WINDOWS = Config::CONFIG['host_os'] =~ /Windows|mswin/

  # JRUBY-2355
  def test_etc_getlogin
    assert_not_nil(Etc.getlogin)
  end
end
