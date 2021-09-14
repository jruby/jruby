# -*- coding: iso-8859-1 -*-
begin
  require 'ant'
rescue Exception => ex
  warn "could not load ant: #{ex.inspect}"
end
require 'rbconfig'

def jruby(java_options = {}, &code)

  java_options[:fork] ||= 'true'
  java_options[:failonerror] ||= 'true'
  java_options[:classname] = 'org.jruby.Main'
  java_options[:maxmemory] ||= JRUBY_LAUNCH_MEMORY

  puts "JAVA options: #{java_options.inspect}"

  ant.path(:id => "jruby.execute.classpath") do
    pathelement :path => "lib/jruby.jar"
  end

  ant.java(java_options) do
    classpath :path => 'lib/jruby.jar'
    sysproperty :key => "jruby.home", :value => BASE_DIR
    instance_eval(&code) if block_given?
  end
end

def mspec(mspec_options = {})
  mspec_options[:compile_mode] ||= 'OFF'
  mspec_options[:jit_threshold] ||= 20
  mspec_options[:jit_max] ||= -1
  mspec_options[:objectspace_enabled] ||= true
  mspec_options[:thread_pooling] ||= false
  mspec_options[:reflection] ||= false
  mspec_options[:format] ||= "m"
  mspec_options[:timeout] ||= 120

  # We can check this property to see whether we failed the run or not
  # TODO a status system property is not implemented
  # java_options[:resultproperty] ||="spec.status.#{mspec_options[:compile_mode]}"

  puts "MSPEC: #{mspec_options.inspect}"
  rm_rf "rubyspec_temp"

  env = {
      "JAVA_OPTS" => "-Demma.verbosity.level=silent",
      "JRUBY_OPTS" => mspec_options[:jruby_opts] || ""
  }

  java_cmd = [ File.join(ENV_JAVA['java.home'], 'bin', 'java') ]
  java_cmd << "-Xmx#{JRUBY_LAUNCH_MEMORY || '1024m'}"
  java_cmd << '-ea'
  java_cmd << '-classpath' ; java_cmd << 'lib/jruby.jar'
  java_cmd << "-Djruby.home=#{BASE_DIR}"

  java_cmd << "-Djruby.launch.inproc=false"
  java_cmd << "-Demma.verbosity.level=silent"

  java_cmd << 'org.jruby.Main'
  # add . to load path so mspec config is found
  java_cmd << "-I . #{MSPEC_BIN} ci"

  java_cmd << "-T -J-ea"
  java_cmd << "-T -J-Djruby.launch.inproc=false"
  java_cmd << "-T -J-Djruby.compile.mode=#{mspec_options[:compile_mode]}"
  java_cmd << "-T -J-Djruby.jit.threshold=#{mspec_options[:jit_threshold]}"
  java_cmd << "-T -J-Djruby.jit.max=#{mspec_options[:jit_max]}"
  java_cmd << "-T -J-Djruby.objectspace.enabled=#{mspec_options[:objectspace_enabled]}"
  java_cmd << "-T -J-Djruby.thread.pool.enabled=#{mspec_options[:thread_pooling]}"
  java_cmd << "-T -J-Djruby.reflection=#{mspec_options[:reflection]}"
  java_cmd << "-T -J-Demma.coverage.out.file=#{TEST_RESULTS_DIR}/coverage.emma"
  java_cmd << "-T -J-Demma.coverage.out.merge=true"
  java_cmd << "-T -J-Demma.verbosity.level=silent"
  java_cmd << "-T -J-XX:MaxMetaspaceSize=768M"
  java_cmd << "-f #{mspec_options[:format]}"
  java_cmd << "--timeout #{mspec_options[:timeout]}"
  java_cmd << "-B #{mspec_options[:spec_config]}" if mspec_options[:spec_config]
  java_cmd << "#{mspec_options[:spec_target]}" if mspec_options[:spec_target]

  sh(env, java_cmd.join(' '))
end

def gem_install(gems, gem_options = "", java_options = {}, &code)
  jruby(java_options) do
    arg :line => "--command maybe_install_gems #{gems} #{gem_options}"
    instance_eval(&code) if block_given?
  end
end
