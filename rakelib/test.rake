require 'rake/testtask'

desc "Alias for spec:ci"
task :spec => "spec:ci"

desc "Alias for test:short"
task :test => "test:short"

if ENV['CI']
  # MRI tests have a different flag for color
  ADDITIONAL_TEST_OPTIONS = "--color=never --tty=no"

  # for normal test/unit tests
  ENV['TESTOPT'] = "--no-use-color"

  # extend timeouts in MRI tests
  ENV['RUBY_TEST_SUBPROCESS_TIMEOUT_SCALE'] = '20'
else
  ADDITIONAL_TEST_OPTIONS = ""
end

AVAILABLE_PROCESSORS = (ENV['JOBS'] || java.lang.Runtime.runtime.available_processors.to_i / 2 + 1)

namespace :test do
  desc "Compile test code"
  task :compile do
    mkdir_p "test/target/test-classes"
    mkdir_p "test/target/test-classes-isolated/java_integration/fixtures/isolated"
    mkdir_p "test/target/test-interfaces-isolated/java_integration/fixtures/isolated"

    classpath = %w[lib/jruby.jar test/target/junit.jar test/target/annotation-api.jar].join(File::PATH_SEPARATOR)
    # try detecting javac - so we use the same Java versions as we're running (JAVA_HOME) with :
    java_home = [ ENV_JAVA['java.home'], File.join(ENV_JAVA['java.home'], '..') ] # in case of jdk/jre
    javac = java_home.map { |home| File.expand_path('bin/javac', home) }.find { |javac| File.exist?(javac) } || 'javac'
    sh "#{javac} -cp #{classpath} -d test/target/test-classes #{Dir['spec/java_integration/fixtures/**/*.java'].to_a.join(' ')}"
    # move the objects that need to be in separate class loaders
    mv "test/target/test-classes/java_integration/fixtures/isolated/classes",
       "test/target/test-classes-isolated/java_integration/fixtures/isolated",
       force: true
    mv "test/target/test-classes/java_integration/fixtures/isolated/interfaces",
       "test/target/test-interfaces-isolated/java_integration/fixtures/isolated",
       force: true
  end

  short_tests = ['jruby', 'mri']
  slow_tests = ['test:slow', 'test:objectspace']
  specs = ['spec:ji', 'spec:compiler', 'spec:ffi', 'spec:regression'];
  long_tests = ["test:tracing"] + short_tests + slow_tests + specs
  all_tests = long_tests.map {|test| test + ':all'}

  desc "Run the short suite: #{short_tests.inspect}"
  task :short => [:compile, *short_tests]

  desc "Run the long suite: #{long_tests.inspect}"
  task :long => [:compile, *long_tests]

  desc "Run the comprehensive suite: #{all_tests}"
  task :all => [:compile, *all_tests]

  desc "Run tests that are too slow for the main suite"
  task :slow_suites => [:compile, *slow_tests]

  task :rake_targets => long_tests
  task :extended => long_tests

  max_meta_size = "-XX:MaxMetaspaceSize"
  get_meta_size = proc do |default_size = 452|
    (ENV['JAVA_OPTS'] || '').index(max_meta_size) || (ENV['JRUBY_OPTS'] || '').index(max_meta_size) ?
        '' : "-J#{max_meta_size}=#{default_size}M"
  end

  compile_flags = {
    :default => :int,
    # interpreter is set to threshold=1 to encourage full builds to run for code called twice
    :int => ["-X-C", "-Xjit.threshold=1", "-Xjit.background=false"],
    # Note: jit.background=false is implied by jit.threshold=0, but we add it here to be sure
    :fullint => ["-X-C", "-Xjit.threshold=0", "-Xjit.background=false"],
    :jit => ["-Xjit.threshold=0", "-Xjit.background=false", get_meta_size.call()],
    :aot => ["-X+C", get_meta_size.call()],
    :all => [:int, :jit, :aot]
  }

  def files_in_file(filename)
    files = []
    File.readlines(filename).each do |line|
      filename = "test/#{line.chomp}.rb"
      files << filename if File.exist? filename
    end
    files
  end

  namespace :mri do
    jruby_opts = {
        # interpreter is set to threshold=1 to encourage full builds to run for code called twice
        int: "--dev -Xjit.threshold=1 -Xjit.background=false",
        :'int:prism' => "-X-C -Xjit.threshold=1 -Xjit.background=false -Xparser.prism",
        fullint: "-X-C -Xjit.threshold=0 -Xjit.background=false",
        jit: "-Xjit.threshold=0 -Xjit.background=false",
        aot: "-X+C -Xjit.background=false #{get_meta_size.call()}"
    }

    mri_suites = [:core, :extra, :stdlib]
    mri_suites = {
      core: "-Xbacktrace.style=mri -Xdebug.fullTrace",
      extra: "--disable-gems -Xbacktrace.style=mri -Xdebug.fullTrace -X+O",
      stdlib: "-Xbacktrace.style=mri -Xdebug.fullTrace",
    }

    mri_suites.each do |suite, extra_jruby_opts|
      files = File.readlines("test/mri.#{suite}.index").grep(/^[^#]\w+/).map(&:chomp).join(' ')

      namespace suite do

        jruby_opts.each do |task, opts|

          task task do
            ENV['JRUBY_OPTS'] = "#{ENV['JRUBY_OPTS']} #{extra_jruby_opts} #{opts}"
            ruby "test/mri/runner.rb #{ADDITIONAL_TEST_OPTIONS} --excludes=test/mri/excludes -q -- #{files}"
          end
          task "#{task}:prism" do
            ENV['JRUBY_OPTS'] = "#{ENV['JRUBY_OPTS']} #{extra_jruby_opts} -Xparser.prism #{opts}"
            ruby "test/mri/runner.rb #{ADDITIONAL_TEST_OPTIONS} --excludes=test/mri/excludes -q -- #{files}"
          end
        end
      end

      # add int shortcut names
      task suite => "test:mri:#{suite}:int"
    end

    task all: jruby_opts.keys
  end
  task mri: ['test:mri:core:int', 'test:mri:extra:int', 'test:mri:stdlib:int']

  permute_tests(:jruby, compile_flags, 'test:compile') do |t|
    files = []
    File.open('test/jruby.index') do |f|
      f.each_line.each do |line|
        filename = "test/#{line.chomp}.rb"
        next unless File.exist? filename
        files << filename
      end
    end
    t.test_files = files
    t.ruby_opts << '-Xaot.loadClasses=true' # disabled by default now
    t.ruby_opts << '-I.'
    t.ruby_opts << '-J-ea'
    t.ruby_opts << '--headless'
    classpath = %w[test test/target/test-classes core/target/test-classes].join(File::PATH_SEPARATOR)
    t.ruby_opts << "-J-cp #{classpath}"
  end

  permute_tests(:slow, compile_flags) do |t|
    files = []
    File.open('test/slow.index') do |f|
      f.each_line.each do |line|
        filename = "test/#{line.chomp}.rb"
        next unless File.exist? filename
        files << filename
      end
    end
    t.test_files = files
    t.test_files = files_in_file 'test/slow.index'
    t.ruby_opts << '-J-ea' << '-I.'
    t.ruby_opts << '-J-cp target/test-classes'
  end

  permute_tests(:objectspace, compile_flags) do |t|
    files = []
    File.open('test/objectspace.index') do |f|
      f.each_line.each do |line|
        filename = "test/#{line.chomp}.rb"
        next unless File.exist? filename
        files << filename
      end
    end
    t.test_files = files
    t.ruby_opts << '-J-ea'
    t.ruby_opts << '-X+O'
  end
  
  def junit(options)
    cp = options[:classpath] or raise "junit tasks must have classpath"
    test = options[:test] or raise "junit tasks must have test"
    
    cmd = "#{ENV_JAVA['java.home']}/bin/java -cp #{cp.join(File::PATH_SEPARATOR)} -Djruby.compat.mode=1.8 junit.textui.TestRunner #{test}"
    
    puts cmd
    system cmd
  end
end
