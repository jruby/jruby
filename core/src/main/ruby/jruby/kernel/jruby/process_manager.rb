module JRuby
  # Implementations of key core process-launching methods using Java 7 process launching APIs.
  module ProcessManager
    java_import java.lang.ProcessBuilder # load early (due GH-1148): not available on GAE
    java_import 'org.jruby.util.ShellLauncher'

    def self.`(command)
      command = command.to_str unless command.kind_of?(String)

      config = ShellLauncher::LaunchConfig.new(JRuby.runtime, [command], false)

      use_shell = JRuby::Util::ON_WINDOWS ? config.should_run_in_shell : false
      use_shell |= ShellLauncher.should_use_shell(command)

      if use_shell
        config.verify_executable_for_shell
      else
        config.verify_executable_for_direct
      end

      pb = ProcessBuilder.new(config.exec_args)
      pb.redirect_input(ProcessBuilder::Redirect::INHERIT)
      pb.redirect_error(ProcessBuilder::Redirect::INHERIT)
      pb.environment
      cwd = JRuby::Util.current_directory
      cwd = cwd.start_with?('uri:classloader:/') ? ENV_JAVA['user.dir'] : cwd
      pb.directory(java.io.File.new(cwd))
      process = pb.start

      pid = ShellLauncher.reflect_pid_from_process(process)
      out = process.input_stream
      result = out.to_io.read
      exit_value = process.wait_for

      # RubyStatus uses real native status now, so we unshift Java's shifted exit status
      JRuby::Util.set_last_exit_status(exit_value << 8, pid)

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
