require 'jruby'

module JRuby
  # Implementations of key core process-launching methods using Java 7 process
  # launching APIs.
  module ProcessManager
    java_import org.jruby.RubyProcess
    java_import org.jruby.util.ShellLauncher
    java_import java.lang.ProcessBuilder
    java_import org.jruby.runtime.builtin.IRubyObject
    java_import org.jruby.platform.Platform

    Redirect = ProcessBuilder::Redirect
    LaunchConfig = ShellLauncher::LaunchConfig
    JFile = java.io.File

    def self.`(command)
      command = command.to_str unless command.kind_of?(String)

      config = LaunchConfig.new(JRuby.runtime, [command].to_java(IRubyObject), false)

      use_shell = Platform::IS_WINDOWS ? config.should_run_in_shell : false
      use_shell ||= ShellLauncher.should_use_shell(command)

      if use_shell
        config.verify_executable_for_shell
      else
        config.verify_executable_for_direct
      end

      pb = ProcessBuilder.new(config.exec_args)
      pb.redirect_input(Redirect::INHERIT)
      pb.redirect_error(Redirect::INHERIT)
      pb.environment(ShellLauncher.get_current_env(JRuby.runtime))
      cwd = JRuby.runtime.current_directory.start_with?('uri:classloader:/') ? ENV_JAVA['user.dir'] : JRuby.runtime.current_directory
      pb.directory(JFile.new(cwd))
      process = pb.start

      pid = ShellLauncher.reflect_pid_from_process(process)
      out = process.input_stream
      result = out.to_io.read
      exit_value = process.wait_for

      status = RubyProcess::RubyStatus.newProcessStatus(JRuby.runtime, exit_value, pid)
      JRuby.runtime.current_context.last_exit_status = status

      result.gsub(/\r\n/, "\n")
    end
  end
end

module Kernel
  module_function
  def `(command)
    JRuby::ProcessManager.`(command)
  end
end
