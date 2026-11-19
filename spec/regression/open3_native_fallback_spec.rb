require 'java'
require 'tmpdir'

# Use java directly: bin/jruby supplies the module openings whose absence we test.
describe 'Open3 without native file descriptor access' do
  io_open = '--add-opens=java.base/java.io=ALL-UNNAMED'
  nio_open = '--add-opens=java.base/sun.nio.ch=ALL-UNNAMED'
  cases = {
    'no module openings' => [[], false],
    'only java.io open' => [[io_open], false],
    'only sun.nio.ch open' => [[nio_open], false],
    'both modules open' => [[io_open, nio_open], !JRuby::Util::ON_WINDOWS],
    'native spawning disabled' => [[io_open, nio_open, '-Djruby.native.popen=false'], false]
  }

  cases.each do |description, (options, native)|
    it "captures streams and exit status with #{description}" do
      java_command = File.join(ENV_JAVA['java.home'], 'bin', 'java')
      home = File.expand_path('../..', __dir__)
      command = [java_command, "-Djruby.home=#{home}", '-cp', File.join(home, 'lib/jruby.jar'),
                 'org.jruby.Main', '-I', File.join(home, 'lib/ruby/stdlib')]
      script = <<~'CODE'
        require 'open3'
        require 'tmpdir'
        expected_native = ARGV.shift == 'true'
        command = ARGV
        raise 'wrong native capability' unless JRuby::Util.native_popen? == expected_native

        Dir.mktmpdir('open3 fallback ') do |dir|
          code = 'STDOUT.sync = true; STDERR.sync = true; print ENV.fetch("OPEN3_TEST"); print STDIN.read; print Dir.pwd; STDERR.print "error"; exit 7'
          output, error, status = Open3.capture3({'OPEN3_TEST' => 'env:'}, *command,
            '-e', code, stdin_data: 'input:', chdir: dir)
          raise output.inspect unless output == "env:input:#{dir}"
          raise error.inspect unless error == 'error'
          raise status.inspect unless status.is_a?(Process::Status) && status.exitstatus == 7

          streams = nil
          Open3.popen2e(*command, '-e', 'STDOUT.sync = true; print STDIN.read; STDERR.print "error"; exit 3') do |input, output, waiter|
            streams = [input, output]
            input.write('input:')
            input.close
            raise 'merged output lost' unless output.read == 'input:error'
            raise 'wrong exit status' unless waiter.value.exitstatus == 3
            raise 'missing pid' unless waiter.pid > 0
          end
          raise 'streams not closed' unless streams.all?(&:closed?)

          output, status = Open3.capture2(*command, '-e', 'print STDIN.read', stdin_data: 'hello')
          raise 'stdout lost' unless output == 'hello'
          raise 'wrong success status' unless status.success?
        end
        puts 'ok'
      CODE

      Dir.mktmpdir('open3-jvm') do |dir|
        log = File.join(dir, 'output')
        args = [java_command, *options, *command.drop(1), '-e', script, native.to_s, *command]
        builder = java.lang.ProcessBuilder.new(args.to_java(:string))
        %w[JAVA_TOOL_OPTIONS JDK_JAVA_OPTIONS _JAVA_OPTIONS JRUBY_OPTS GEM_HOME GEM_PATH].each { |key| builder.environment.remove(key) }
        builder.redirectErrorStream(true)
        builder.redirectOutput(java.io.File.new(log))
        process = builder.start
        begin
          completed = process.waitFor(120, java.util.concurrent.TimeUnit::SECONDS)
          expect(completed).to be(true), "child JVM timed out: #{File.read(log)}"
          expect(process.exitValue).to eq(0), File.read(log)
          expect(File.read(log)).to include("ok\n")
        ensure
          if process.isAlive
            process.destroyForcibly
            process.waitFor(10, java.util.concurrent.TimeUnit::SECONDS)
          end
        end
      end
    end
  end
end
