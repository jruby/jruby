require 'test/unit'
require 'test/jruby/test_helper'
require 'fileutils'
require 'rbconfig'
require 'shellwords'
require 'tmpdir'

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
    assert_match(/exit 1/, @first_status.to_s)
    assert_match(/exit 2/, @second_status.to_s)
  end

  def test_process_status_exitstatus
    assert_equal 1, @first_status.exitstatus
    assert_equal 2, @second_status.exitstatus
  end

  def test_process_exited
    assert_equal true, @first_status.exited?
    assert_equal true, @second_status.exited?
  end

  def test_process_signaled
    assert_equal false, @first_status.signaled?
    assert_equal false, @second_status.signaled?
  end

  def test_process_stopsig
    assert_equal nil, @first_status.stopsig
    assert_equal nil, @second_status.stopsig
  end

  def test_process_termsig
    assert_equal nil, @first_status.termsig
    assert_equal nil, @second_status.termsig
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

  def test_gid_windows
    assert_equal 0, Process.gid
    assert_equal 0, Process.egid
  end if WINDOWS

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
    # NOTE: prints a warning but does not raise
    # warning: Process#getrlimit not supported on this platform
    #assert_raise(NotImplementedError) { Process.getrlimit(100) }
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
  end if WINDOWS

  def test_chdir_option_does_not_allow_shell_injection
    omit 'requires POSIX shell' if WINDOWS

    Dir.mktmpdir('jruby_chdir_test') do |tmpdir|
      marker = File.join(tmpdir, 'injected_marker')
      malicious_dir = "missing' ; touch #{Shellwords.escape(marker)} #"

      system('echo safe', chdir: malicious_dir, out: File::NULL, err: File::NULL)

      assert_false File.exist?(marker), 'command injection via chdir: path executed'
    end
  end

  def test_chdir_option_does_not_run_command_when_chdir_fails
    omit 'requires POSIX shell' if WINDOWS

    Dir.mktmpdir('jruby_chdir_test') do |tmpdir|
      marker = File.join(tmpdir, 'failed_chdir_marker')
      missing_dir = File.join(tmpdir, 'missing')

      system("echo safe; touch #{Shellwords.escape(marker)}", chdir: missing_dir, out: File::NULL, err: File::NULL)

      assert_false File.exist?(marker), 'command ran even though chdir: target was missing'
    end
  end

  def test_chdir_option_handles_shell_special_characters_in_path
    omit 'requires POSIX shell' if WINDOWS

    Dir.mktmpdir('jruby_chdir_test') do |tmpdir|
      ["dir with ' quote $dollar ; semicolon", '-P'].each do |name|
        dir = File.join(tmpdir, name)
        FileUtils.mkdir_p(dir)

        assert system('true', chdir: dir, out: File::NULL, err: File::NULL)

        output = IO.popen('pwd', chdir: dir, &:read).strip
        assert_equal File.realpath(dir), File.realpath(output)
      end
    end
  end

  # On macOS CLOCK_MONOTONIC keeps counting while the system sleeps, but System.nanoTime() doesn't, so read the
  # same clock CRuby does: https://github.com/jruby/jruby/issues/9982
  if RbConfig::CONFIG['host_os'] =~ /darwin/
    require 'ffi'

    module DarwinClock
      extend FFI::Library
      ffi_lib FFI::Library::LIBC
      attach_function :clock_gettime_nsec_np, [:int], :uint64

      # Darwin's clockid_t for CLOCK_MONOTONIC, since JRuby's Process::CLOCK_MONOTONIC is a Symbol
      CLOCK_MONOTONIC = 6
    end

    def test_clock_monotonic_counts_time_asleep_on_macos
      before = DarwinClock.clock_gettime_nsec_np(DarwinClock::CLOCK_MONOTONIC)
      monotonic = Process.clock_gettime(Process::CLOCK_MONOTONIC, :nanosecond)
      after = DarwinClock.clock_gettime_nsec_np(DarwinClock::CLOCK_MONOTONIC)

      assert_operator monotonic, :>=, before
      assert_operator monotonic, :<=, after
      assert_equal 1e-6, Process.clock_getres(Process::CLOCK_MONOTONIC)
    end

    # The native clock is only for runtimes that allow native access, even when the JVM-wide option does
    def test_clock_monotonic_without_native_access_on_macos
      config = org.jruby.RubyInstanceConfig.new
      config.native_enabled = false
      runtime = org.jruby.Ruby.new_instance(config)

      assert_equal 1e-9, runtime.eval_scriptlet('Process.clock_getres(Process::CLOCK_MONOTONIC)')
    ensure
      runtime&.tear_down
    end
  end

end
