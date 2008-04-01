require 'test/unit'
require 'rbconfig'

class TestProcess < Test::Unit::TestCase
  WINDOWS = Config::CONFIG['host_os'] =~ /Windows|mswin/
  def setup
    @shell = Config::CONFIG['SHELL']
    @shellcmd = "#@shell " + (Config::CONFIG['host_os'] =~ /Windows|mswin/ ? "/c" : "-c")
    system(%{#@shellcmd "exit 1"})
    @first_status = $?
    system(%{#@shellcmd "exit 2"})
    @second_status = $?
  end

  def test_process_status_returned_from_dollar_query
    assert_kind_of Process::Status, @first_status
    assert_kind_of Process::Status, @second_status
  end

  def test_process_status_to_i
    assert_equal 256, @first_status.to_i
    assert_equal 512, @second_status.to_i
  end

  def test_process_status_to_s
    assert_equal "256", @first_status.to_s
    assert_equal "512", @second_status.to_s
  end

  def test_process_status_exitstatus
    assert_equal 1, @first_status.exitstatus
    assert_equal 2, @second_status.exitstatus
  end

  def test_process_times
    tms = nil
    assert_nothing_raised {
      tms = Process.times
    }
    assert tms.utime
    assert tms.stime
    assert tms.cutime
    assert tms.cstime
    assert tms.utime > 0
  end
  
  def test_host_process
    unless Config::CONFIG['host_os'] =~ /Windows|mswin/ || !File.exist?("bin/jruby")
      assert_equal "1", %x{sh -c 'bin/jruby -e "exit 1" ; echo $?'}.strip
    end
  end

  if (WINDOWS)
    # JRUBY-2352
    def test_not_implemented_methods_on_windows
      # The goal here is to make sure that those "weird"
      # POSIX methods don't break JRuby, since there were
      # numerous regressions in this area.
      assert_raise(NotImplementedError) { Process.uid }
      assert_raise(NotImplementedError) { Process.uid = 5 }
      assert_raise(NotImplementedError) { Process.euid }
      assert_raise(NotImplementedError) { Process.euid = 5 }
      assert_raise(NotImplementedError) { Process.egid }
      assert_raise(NotImplementedError) { Process.egid = 5 }
      assert_raise(NotImplementedError) { Process.getpgid(100) }
      assert_raise(NotImplementedError) { Process.setpgid(100, 555) }
      assert_raise(NotImplementedError) { Process.setpriority(100, 100, 100) }
      assert_raise(NotImplementedError) { Process.getpriority(100, 100) }
    end
  end
end
