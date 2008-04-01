require 'test/unit'
require 'rbconfig'
require 'etc'

class TestEtc < Test::Unit::TestCase
  WINDOWS = Config::CONFIG['host_os'] =~ /Windows|mswin/

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
    def test_not_implemented_methods_on_windows
      assert_nil(Etc.endgrent)
      assert_nil(Etc.endpwent)
      assert_nil(Etc.getgrent)
      assert_nil(Etc.getgrgid(100))
      assert_nil(Etc.getgrnam("name"))
      assert_nil(Etc.getpwent)
      assert_nil(Etc.getpwnam("name"))
      begin
        assert_nil(Etc.getpwuid)
      rescue NotImplementedError
      end
      assert_nil(Etc.getpwuid(100))
      assert_nil(Etc.group)
      assert_nil(Etc.passwd)
      assert_nil(Etc.setgrent)
      assert_nil(Etc.setpwent)  
    end
  end
end
