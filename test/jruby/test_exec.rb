require 'test/unit'
require 'rbconfig'
require 'tempfile'

class TestExec < Test::Unit::TestCase
  if RbConfig::CONFIG['host_os'] =~ /Windows|mswin/
    # GH-6745
    def test_exec_finds_bat_files_on_windows
      Tempfile.open([__method__.to_s, ".bat"]) do |f|
        path = File.dirname(f)
        f.write "@echo hello"
        f.flush
        assert_equal "hello\n", `jruby -e "exec '#{f.path.gsub(".bat", "")}'"`
      end
    end
  end
end