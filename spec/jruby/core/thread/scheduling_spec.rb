require 'java'
require 'jruby'
require 'open3'
require 'rbconfig'

describe 'Ruby thread scheduling' do
  [['process waits', 1, 'wait.rb'], ['fiber startup', 2, 'fiber_startup.rb']].each do |behavior, parallelism, file|
    [false, true].each do |virtual|
      it "supports #{behavior} with thread.vthread=#{virtual}" do
        env = %w[
          JRUBY_OPTS JAVA_OPTS JAVA_TOOL_OPTIONS _JAVA_OPTIONS JDK_JAVA_OPTIONS
          RUBYOPT RUBYLIB
        ].to_h { |key| [key, nil] }
        config = JRuby.runtime.instance_config
        options = [
          "-J-Djruby.thread.vthread=#{virtual}",
          "-J-Djdk.virtualThreadScheduler.parallelism=#{parallelism}",
          "-J-Djdk.virtualThreadScheduler.maxPoolSize=#{parallelism}",
          "-Xcompile.mode=#{config.compile_mode}",
          "-Xjit.threshold=#{config.jit_threshold}"
        ]
        fixture = File.expand_path("fixtures/#{file}", __dir__)
        Open3.popen3(env, RbConfig.ruby, *options, fixture, virtual.to_s) do |stdin, stdout, stderr, child|
          stdin.close
          # Bound a deadlocked child without making scheduling speed an assertion.
          completed = child.join(30)
          Process.kill('KILL', child.pid) unless completed
          output = stdout.read
          errors = stderr.read
          diagnostic = completed ? "#{output}\n#{errors}" : "child deadlocked (30-second harness deadline)\n#{output}\n#{errors}"
          expect(child.value.success?).to eq(true), diagnostic
        end
      end
    end
  end
end
