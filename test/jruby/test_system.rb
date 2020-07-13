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
  def test_system_with_conflicting_dir; require 'open3'
    FileUtils.mkdir_p '6960-extra_path/java'

    path = ENV['PATH']
    if ENV['JAVA_HOME'] && defined? JRUBY_VERSION
      unless path.split(File::PATH_SEPARATOR).include?(ENV['JAVA_HOME'])
        path = "#{ENV['JAVA_HOME']}#{File::PATH_SEPARATOR}#{path}"
      end
    end

    ENV['PATH'] = "6960-extra_path#{File::PATH_SEPARATOR}#{path}"
    Open3.popen3("java -version") do |i, o, e, t|
      out = e.read || ''
      out = o.read if out.strip.empty?
      puts out.inspect
      assert_match(/java|openjdk/i, out)
      assert_equal(t.value, 0)
    end
  ensure
    FileUtils.rm_rf '6960-extra_path'
    ENV['PATH'] = path
  end

end
