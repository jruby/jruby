# -*- coding: iso-8859-1 -*-
begin
  require 'ant'
rescue Exception => ex
  warn "could not load ant: #{ex.inspect}"
end
require 'rbconfig'

def initialize_paths
  ant.path(:id => "jruby.execute.classpath") do
    pathelement :path => "lib/jruby.jar"
  end

  ant.path(:id => "test.class.path") do
    pathelement :path => File.join(BUILD_LIB_DIR, 'junit.jar')
    pathelement :path => File.join(BUILD_LIB_DIR, 'annotation-api.jar')
    pathelement :path => File.join(BUILD_LIB_DIR, 'livetribe-jsr223.jar')
    pathelement :path => File.join(BUILD_LIB_DIR, 'bsf.jar')
    pathelement :path => File.join(BUILD_LIB_DIR, 'commons-logging.jar')
    #  pathelement :path => "${java.class.path}"/>
    pathelement :path => File.join(LIB_DIR, 'jruby.jar')
    pathelement :location => TEST_CLASSES_DIR
    pathelement :path => File.join(TEST_DIR, 'jruby', 'requireTest.jar')
    pathelement :location => TEST_DIR
  end
end

def jruby(java_options = {}, &code)
  initialize_paths

  java_options[:fork] ||= 'true'
  java_options[:failonerror] ||= 'true'
  java_options[:classname] = 'org.jruby.Main'
  java_options[:maxmemory] ||= JRUBY_LAUNCH_MEMORY

  puts "JAVA options: #{java_options.inspect}"

  ant.java(java_options) do
    classpath :path => 'lib/jruby.jar'
    sysproperty :key => "jruby.home", :value => BASE_DIR
    instance_eval(&code) if block_given?
  end
end

def jrake(dir, targets, java_options = {}, &code)
  java_options[:dir] = dir
  jruby(java_options) do
    classpath :refid => "test.class.path"
    instance_eval(&code) if block_given?
    arg :line => "-S rake #{targets}"
  end
end

def mspec(mspec_options = {}, java_options = {}, &code)
  java_options[:dir] ||= BASE_DIR
  java_options[:maxmemory] ||= JRUBY_LAUNCH_MEMORY

  mspec_options[:compile_mode] ||= 'OFF'
  mspec_options[:jit_threshold] ||= 20
  mspec_options[:jit_max] ||= -1
  mspec_options[:objectspace_enabled] ||= true
  mspec_options[:thread_pooling] ||= false
  mspec_options[:reflection] ||= false
  mspec_options[:format] ||= "m"
  mspec_options[:timeout] ||= 120
  ms = mspec_options

  # We can check this property to see whether we failed the run or not
  java_options[:resultproperty] ||="spec.status.#{mspec_options[:compile_mode]}"

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

    arg :line => "#{MSPEC_BIN} ci"
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
    arg :line => "#{ms[:spec_target]}" if ms[:spec_target]
  end
end

def gem_install(gems, gem_options = "", java_options = {}, &code)
  jruby(java_options) do
    arg :line => "--command maybe_install_gems #{gems} #{gem_options}"
    instance_eval(&code) if block_given?
  end
end
