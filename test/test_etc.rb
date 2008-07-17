require 'test/unit'
require 'rbconfig'
require 'etc'

class TestEtc < Test::Unit::TestCase
  WINDOWS = Config::CONFIG['host_os'] =~ /Windows|mswin/
  
  def assert_nil_or_not_implemented
    assert_nil(yield)
  rescue NotImplementedError
  end

  # JRUBY-2355
  def test_etc_getlogin
    # TODO: excliding this test case since it fails
    # for me on Linux when executed from within Ant build 
    # for some reason
    # 
    # assert_not_nil(Etc.getlogin)
  end

  if (WINDOWS)
    # JRUBY-2356
    # TODO: see JRUBY-2820: Most Etc methods behave diferently
    # on Windows under x32 and x64 JVMs
    def test_not_implemented_methods_on_windows
      assert_nil_or_not_implemented { Etc.endgrent }
      assert_nil_or_not_implemented { Etc.endpwent }
      assert_nil_or_not_implemented { Etc.getgrent }
      assert_nil_or_not_implemented { Etc.getgrgid(100) }
      assert_nil_or_not_implemented { Etc.getgrnam("name") }
      assert_nil_or_not_implemented { Etc.getpwent }
      assert_nil_or_not_implemented { Etc.getpwnam("name") }
      assert_nil_or_not_implemented { Etc.getpwuid }
      assert_nil_or_not_implemented { Etc.getpwuid(100) }
      assert_nil_or_not_implemented { Etc.group }
      assert_nil_or_not_implemented { Etc.passwd }
      assert_nil_or_not_implemented { Etc.setgrent }
      assert_nil_or_not_implemented { Etc.setpwent }
    end
  end
end
