require 'test/unit'
require 'test/test_helper'
require 'rbconfig'

class TestProcess < Test::Unit::TestCase
  include TestHelper

  def setup
    @shell = RbConfig::CONFIG['SHELL']
    @shellcmd = "#@shell " + (RbConfig::CONFIG['host_os'] =~ /Windows|mswin/ ? "/c" : "-c")
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

  def test_process_exited
    assert_equal false, @first_status.exited?
    assert_equal false, @second_status.exited?
  end

  def test_process_signaled
    assert_equal true, @first_status.signaled?
    assert_equal true, @second_status.signaled?
  end

  def test_process_stopsig
    assert_equal 1, @first_status.stopsig
    assert_equal 2, @second_status.stopsig
  end

  def test_process_termsig
    assert_equal 1, @first_status.termsig
    assert_equal 2, @second_status.termsig
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
    unless RbConfig::CONFIG['host_os'] =~ /Windows|mswin/ || !File.exist?("bin/jruby")
      assert_equal "1", %x{sh -c 'bin/jruby -e "exit 1" ; echo $?'}.strip
    end
  end

  if (WINDOWS)
    def test_gid_windows
      assert_equal 0, Process.gid
      assert_equal 0, Process.egid
    end

    # JRUBY-2352
    def test_not_implemented_methods_on_windows
      # The goal here is to make sure that those "weird"
      # POSIX methods don't break JRuby, since there were
      # numerous regressions in this area.
      assert_raise(NotImplementedError) { Process.uid = 5 }
      assert_raise(NotImplementedError) { Process.gid = 5 }

      # TODO: JRUBY-2705, doesn't work on x64 JVM
      assert_equal 0, Process.euid unless WINDOWS_JVM_64

      assert_raise(NotImplementedError) { Process.euid = 5 }
      assert_raise(NotImplementedError) { Process.egid = 5 }
      assert_raise(NotImplementedError) { Process.getpgid(100) }
      assert_raise(NotImplementedError) { Process.setpgid(100, 555) }
      assert_raise(NotImplementedError) { Process.setpriority(100, 100, 100) }
      assert_raise(NotImplementedError) { Process.getpriority(100, 100) }
      assert_raise(NotImplementedError) { Process.setrlimit(100, 100) }
      assert_raise(NotImplementedError) { Process.getrlimit(100) }
      assert_raise(NotImplementedError) { Process.groups }
      assert_raise(NotImplementedError) { Process.groups = [] }
      assert_raise(NotImplementedError) { Process.maxgroups }
      assert_raise(NotImplementedError) { Process.maxgroups = 100 }
      assert_raise(NotImplementedError) { Process.initgroups(100, 100) }

      # TODO: JRUBY-2639, doesn't work on x64 JVM
      assert_equal 0, Process.ppid unless WINDOWS_JVM_64

      # TODO: temporal (JRUBY-2354)
      assert_raise(NotImplementedError) { Process.wait }
      assert_raise(NotImplementedError) { Process.wait2 }
      assert_raise(NotImplementedError) { Process.waitpid }
      assert_raise(NotImplementedError) { Process.waitpid2 }
      assert_raise(NotImplementedError) { Process.waitall }
    end
  end
end
