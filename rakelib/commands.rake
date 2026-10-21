# -*- coding: iso-8859-1 -*-
require 'rbconfig'
require 'shellwords'

module JRuby
  module Rake
    class JavaCommand
      def self.classpaths
        {
          "test.class.path" => [
            File.join(BUILD_LIB_DIR, 'junit.jar'),
            File.join(BUILD_LIB_DIR, 'annotation-api.jar'),
            File.join(BUILD_LIB_DIR, 'livetribe-jsr223.jar'),
            File.join(BUILD_LIB_DIR, 'bsf.jar'),
            File.join(BUILD_LIB_DIR, 'commons-logging.jar'),
            File.join(LIB_DIR, 'jruby.jar'),
            TEST_CLASSES_DIR,
            File.join(TEST_DIR, 'jruby', 'requireTest.jar'),
            TEST_DIR
          ]
        }
      end

      def self.run(options, &code)
        java = new
        java.instance_eval(&code) if block_given?

        spawn_options = {}
        spawn_options[:chdir] = options[:dir] if options[:dir]
        spawn_options[:out] = options[:output] if options[:output]
        spawn_options[:err] = [:child, :out] if options[:output]

        system(java.env, *java.command(options), spawn_options)
        status = $?.exitstatus

        # A non-zero exit aborts the rake task, so callers don't need to check exit codes.
        fail "Java returned: #{status}" if options[:failonerror] == 'true' && status != 0
      end

      def initialize
        @jvmargs, @sysproperties, @classpath, @args, @env = [], [], [], [], {}
      end

      def classpath(options)
        paths = options[:refid] ? self.class.classpaths.fetch(options[:refid]) : options[:path].split(File::PATH_SEPARATOR)
        @classpath.concat paths
      end

      def jvmarg(options)
        @jvmargs.concat Shellwords.split(options[:line])
      end

      def sysproperty(options)
        @sysproperties << "-D#{options[:key]}=#{options[:value]}"
      end

      def arg(options)
        @args.concat Shellwords.split(options[:line])
      end

      def env(options = nil)
        return @env unless options
        @env[options[:key]] = options[:value]
      end

      def command(options)
        # use absolute paths and drop entries that do not exist
        classpath = @classpath.
          map { |path| File.expand_path(path, BASE_DIR) }.
          select { |path| File.exist?(path) }.
          uniq

        cmd = [File.join(ENV_JAVA['java.home'], 'bin', 'java'), *@jvmargs]
        cmd << "-Xmx#{options[:maxmemory]}" if options[:maxmemory]
        cmd.concat @sysproperties
        cmd.push '-classpath', classpath.join(File::PATH_SEPARATOR) unless classpath.empty?
        cmd << options[:classname]
        cmd.concat @args
      end
    end
  end
end

def jruby(java_options = {}, &code)
  java_options[:fork] ||= 'true'
  java_options[:failonerror] ||= 'true'
  java_options[:classname] = 'org.jruby.main.Main'
  java_options[:maxmemory] ||= JRUBY_LAUNCH_MEMORY

  puts "JAVA options: #{java_options.inspect}"

  JRuby::Rake::JavaCommand.run(java_options) do
    classpath :path => 'lib/jruby.jar'
    sysproperty :key => "jruby.home", :value => BASE_DIR
    instance_eval(&code) if block_given?
  end
end

def mspec(mspec_options = {}, java_options = {}, &code)
  java_options[:dir] ||= BASE_DIR
  java_options[:maxmemory] ||= JRUBY_LAUNCH_MEMORY

  mspec_options[:command] ||= 'ci'
  mspec_options[:compile_mode] ||= 'OFF'
  mspec_options[:jit_threshold] ||= 20
  mspec_options[:jit_max] ||= -1
  mspec_options[:objectspace_enabled] ||= true
  mspec_options[:thread_pooling] ||= false
  mspec_options[:reflection] ||= false
  mspec_options[:format] ||= "d"
  mspec_options[:timeout] ||= 120
  ms = mspec_options

  puts "MSPEC: #{ms.inspect}"
  rm_rf "rubyspec_temp"

  jruby(java_options) do
    classpath :refid => "test.class.path"
    jvmarg :line => "-ea"
    sysproperty :key => "jruby.launch.inproc", :value => "false"
    sysproperty :key => "emma.verbosity.level", :value=> "silent"

    env :key => "JAVA_OPTS", :value => "-Demma.verbosity.level=silent"
    env :key => "JRUBY_OPTS", :value => ms[:jruby_opts] || ""
    # launch in the same mode we're testing, since config is loaded by top process

    # add . to load path so mspec config is found
    arg :line => "-I ."

    arg :line => "#{MSPEC_BIN} #{ms[:command]}"
    arg :line => "-T -J-ea"
    arg :line => "-T -J-Djruby.launch.inproc=false"
    arg :line => "-T -J-Djruby.compile.mode=#{ms[:compile_mode]}"
    arg :line => "-T -J-Djruby.jit.threshold=#{ms[:jit_threshold]}"
    arg :line => "-T -J-Djruby.jit.max=#{ms[:jit_max]}"
    arg :line => "-T -J-Djruby.objectspace.enabled=#{ms[:objectspace_enabled]}"
    arg :line => "-T -J-Djruby.thread.pool.enabled=#{ms[:thread_pooling]}"
    arg :line => "-T -J-Djruby.reflection=#{ms[:reflection]}"
    arg :line => "-T -J-Demma.coverage.out.file=#{TEST_RESULTS_DIR}/coverage.emma"
    arg :line => "-T -J-Demma.coverage.out.merge=true"
    arg :line => "-T -J-Demma.verbosity.level=silent"
    arg :line => "-T -J-XX:MaxMetaspaceSize=768M"
    arg :line => "-f #{ms[:format]}"
    arg :line => "--timeout #{ms[:timeout]}"
    arg :line => "-B #{ms[:spec_config]}" if ms[:spec_config]
    (ms[:tags] || []).each do |tag|
      arg :line => "-g #{tag}"
    end
    arg :line => "#{ms[:spec_target]}" if ms[:spec_target]
  end
end
