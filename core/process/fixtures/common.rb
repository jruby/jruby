module ProcessSpecs
  class Daemonizer
    attr_reader :input, :data, :signal

    def initialize
      @script = fixture __FILE__, "daemon.rb"
      @input = tmp("process_daemon_input_file")
      @data = tmp("process_daemon_data_file")
      @signal = tmp("process_daemon_signal_file")
      @args = []
    end

    def wait_for_daemon
      10.times do
        return true if File.exist? @signal and
                       File.exist? @data and
                       File.size? @data
        sleep 0.1
      end

      return false
    end

    def invoke(behavior, arguments=[])
      args = Marshal.dump(arguments).unpack("H*")
      args << @input << @data << @signal << behavior

      ruby_exe @script, :args => args

      wait_for_daemon

      return unless File.exist? @data

      File.open(@data, "rb") { |f| return f.read.chomp }
    end
  end

  class Signalizer
    attr_reader :pid_file, :pid

    def initialize(scenario=nil, ruby_exe=nil)
      @script = fixture __FILE__, "kill.rb"
      @pid_file = tmp("process_kill_signal_file")
      rm_r @pid_file

      @thread = Thread.new do
        args = [@pid_file, scenario, ruby_exe]
        @result = ruby_exe @script, :args => args
      end
      Thread.pass until File.exist? @pid_file
      while @pid.nil? || @pid == 0
        @pid = IO.read(@pid_file).chomp.to_i
      end
    end

    def wait_on_result
      # Ensure the process exits
      begin
        Process.kill :TERM, pid
      rescue Errno::ESRCH
        # Ignore the process not existing
      end

      @thread.join
    end

    def cleanup
      wait_on_result
      rm_r pid_file
    end

    def result
      wait_on_result
      @result.chomp if @result
    end
  end
end
