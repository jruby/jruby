require 'java'
require 'jruby'
require 'open3'
require 'rbconfig'

describe 'Ruby Thread backing' do
  [false, true].each do |virtual|
    it "supports Ruby thread behavior with thread.vthread=#{virtual}" do
      # RubyThread's factory is static: a sub-runtime in this JVM cannot change it.
      # Set the property at JVM startup, independently of the parent suite's mode.
      env = %w[
        JRUBY_OPTS JAVA_OPTS JAVA_TOOL_OPTIONS _JAVA_OPTIONS JDK_JAVA_OPTIONS
        RUBYOPT RUBYLIB
      ].to_h { |key| [key, nil] }
      config = JRuby.runtime.instance_config
      options = [
        "-J-Djruby.thread.vthread=#{virtual}",
        "-Xcompile.mode=#{config.compile_mode}",
        "-Xjit.threshold=#{config.jit_threshold}"
      ]
      fixture = File.expand_path('fixtures/backing.rb', __dir__)
      stdout, stderr, status = Open3.capture3(env, RbConfig.ruby, *options, fixture, virtual.to_s)
      expect(status.success?).to eq(true), "#{stdout}\n#{stderr}"
    end
  end
end
