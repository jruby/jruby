## Make sure that Kernel#system command works.

require 'test/unit'
require 'rbconfig'

class TestSystem < Test::Unit::TestCase
  def test_system_on_windows
    # JRUBY-5110
    if RbConfig::CONFIG['host_os'] =~ /Windows|mswin/
      ENV['PATH'] += (';' + File.join(File.dirname(__FILE__), "..\\tool\\nailgun"))
      assert(system 'ng --nailgun-version > $null')
    end
  end
end
