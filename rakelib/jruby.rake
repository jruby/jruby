require 'ant'

# Determine if we need to put a 32 or 64 bit flag to the command-line
# based on what java reports as the hardward architecture.
def jvm_model
  # My windows machine does not do this?  Can this happen?
  return nil if ENV_JAVA['os.family'] == "windows"

  case ENV_JAVA['os.arch']
  when 'amd64', 'x86_64', 'sparcv9', 's390x' then
    '-d64'
  when 'i386', 'x86', 'powerpc', 'ppc', 'sparc' then
    '-d32'
  else
    nil
  end
end
JVM_MODEL = jvm_model

ant.path(:id => "build.classpath") do
  fileset :dir => BUILD_LIB_DIR, :includes => "*.jar"
end

ant.path(:id => "jruby.execute.classpath") do
  path :refid => "build.classpath"
  pathelement :path => JRUBY_CLASSES_DIR
end

ant.path(:id => "test.class.path") do
  pathelement :path => File.join(BUILD_LIB_DIR, 'junit.jar')
  pathelement :path => File.join(BUILD_LIB_DIR, 'livetribe-jsr223-2.0.6.jar')
  pathelement :path => File.join(BUILD_LIB_DIR, 'bsf.jar')
  pathelement :path => File.join(BUILD_LIB_DIR, 'commons-logging-1.1.1.jar')
#  pathelement :path => "${java.class.path}"/>
  pathelement :path => File.join(LIB_DIR, 'jruby.jar')
  pathelement :location => TEST_CLASSES_DIR
  pathelement :path => File.join(TEST_DIR, 'requireTest.jar')
  pathelement :location => TEST_DIR
end

def jruby(java_options = {}, &code)
  java_options[:fork] ||= 'true'
  java_options[:failonerror] ||= 'true'
  java_options[:classname] = 'org.jruby.Main'
  ant.java(java_options) do
    classpath :refid => 'build.classpath'
    classpath :path => JRUBY_CLASSES_DIR
    jvmarg :line => JVM_MODEL if JVM_MODEL
    sysproperty :key => "jruby.home", :value => BASE_DIR
    instance_eval(&code) if block_given?
  end
end

def jrake(dir, targets, java_options = {}, &code)
  java_options[:maxmemory] ||= JRUBY_LAUNCH_MEMORY
  java_options[:dir] = dir
  jruby(java_options) do
    classpath :refid => "test.class.path"
    instance_eval(&code) if block_given?
    arg :line => "-S rake #{targets}"
  end
end

def mspec(mspec_options = {}, java_options = {}, &code)
  java_options[:maxmemory] ||= JRUBY_LAUNCH_MEMORY
  java_options[:dir] = BASE_DIR
  java_options[:failonerror] ||= 'false'

  mspec_options[:compile_mode] ||= 'OFF'
  mspec_options[:jit_threshold] ||= 20
  mspec_options[:jit_max] ||= -1
  mspec_options[:objectspace_enabled] ||= true
  mspec_options[:thread_pooling] ||= false
  mspec_options[:reflection] ||= false
  mspec_options[:compat] ||= "RUBY1_8"
  ms = mspec_options

  puts "MSPEC: #{ms.inspect}"

  jruby(java_options) do
    classpath :refid => "test.class.path"
    jvmarg :line => "-ea"
    sysproperty :key => "jruby.launch.inproc", :value => "false"
    sysproperty :key => "emma.verbosity.level", :value=> "silent"

    env :key => "JAVA_OPTS", :value => "-Demma.verbosity.level=silent"
    arg :line => "#{MSPEC_BIN} ci"
    arg :line => "-T -J-ea"
    arg :line => "-T -J-Djruby.launch.inproc=false"
    arg :line => "-T -J-Djruby.compile.mode=#{ms[:compile_mode]}"
    arg :line => "-T -J-Djruby.jit.threshold=#{ms[:jit_threshold]}"
    arg :line => "-T -J-Djruby.jit.max=#{ms[:jit_max]}"
    arg :line => "-T -J-Djruby.objectspace.enabled=#{ms[:objectspace_enabled]}"
    arg :line => "-T -J-Djruby.thread.pool.enabled=#{ms[:thread_pooling]}"
    arg :line => "-T -J-Djruby.reflection=#{ms[:reflection]}"
    arg :line => "-T -J-Djruby.compat.version=#{ms[:compat]}"
    arg :line => "-T -J-Demma.coverage.out.file=#{TEST_RESULTS_DIR}/coverage.emma"
    arg :line => "-T -J-Demma.coverage.out.merge=true"
    arg :line => "-T -J-Demma.verbosity.level=silent"
    arg :line => "${spec.jvm.model.option}"
    arg :line => "-f m"
    arg :line => "-B #{ms[:spec_config]}" if ms[:spec_config]
  end
end
