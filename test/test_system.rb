## Make sure that Kernel#system command works.

require 'test/unit'
require 'rbconfig'

class TestSystem < Test::Unit::TestCase
  # JRUBY-5110
  if RbConfig::CONFIG['host_os'] =~ /Windows|mswin/
    def test_system_on_windows
      ENV['PATH'] += (';' + File.join(File.dirname(__FILE__), "..\\tool\\nailgun"))
      assert(system 'ng --nailgun-version > $null')
    end
  end

  # JRUBY-6960
  def test_system_with_conflicting_dir
    FileUtils.mkdir_p 'extra_path/java'
    ENV['PATH'] = "extra_path#{File::PATH_SEPARATOR}#{ENV['PATH']}"
    assert(system 'java -version')
  ensure
    FileUtils.rm_rf 'extra_path'
  end

end
