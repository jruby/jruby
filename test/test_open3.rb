
require 'rbconfig'
require 'test/unit'

require 'open3'

class TestOpen3 < Test::Unit::TestCase
  WINDOWS = Config::CONFIG['host_os'] =~ /Windows|mswin/

  # JRUBY-3071
  def test_popen3_reads_without_seeking
    shell_cmd = WINDOWS ? 'dir' : 'ls'
    assert_nothing_raised() do
      Open3.popen3(shell_cmd) do |date_in, date_out, date_err|
        date_out.read
      end
    end
  end

end
