require 'rake/testtask'

desc "Alias for spec:ci"
task :spec => "spec:ci"

desc "Alias for test:short"
task :test => "test:short"

if ENV['CI']
  # MRI tests have a different flag for color
  ADDITIONAL_TEST_OPTIONS = "-v --color=never --tty=no"

  # for normal test/unit tests
  ENV['TESTOPT'] = "-v --no-use-color"
else
  ADDITIONAL_TEST_OPTIONS = ""
end

namespace :test do
  desc "Compile test code"
  task :compile do
    mkdir_p "test/target/test-classes"
    classpath = %w[lib/jruby.jar test/target/junit.jar].join(File::PATH_SEPARATOR)
    # try detecting javac - so we use the same Java versions as we're running (JAVA_HOME) with :
    java_home = [ ENV_JAVA['java.home'], File.join(ENV_JAVA['java.home'], '..') ] # in case of jdk/jre
    javac = java_home.map { |home| File.expand_path('bin/javac', home) }.find { |javac| File.exist?(javac) } || 'javac'
    sh "#{javac} -cp #{classpath} -d test/target/test-classes #{Dir['spec/java_integration/fixtures/**/*.java'].to_a.join(' ')}"
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

  desc "Run tracing tests"
  task :tracing do
    Rake::TestTask.new('test:tracing') do |t|
      t.pattern = 'test/tracing/test_*.rb'
      t.verbose = true
      t.ruby_opts << '-J-ea'
      t.ruby_opts << '--debug'
      t.ruby_opts << '--disable-gems'
    end
  end

  max_meta_size = ENV_JAVA['java.specification.version'] > '1.7' ? '-XX:MaxMetaspaceSize' : '-XX:MaxPermSize'
  get_meta_size = proc do |default_size = 452|
    (ENV['JAVA_OPTS'] || '').index(max_meta_size) || (ENV['JRUBY_OPTS'] || '').index(max_meta_size) ?
        '' : "-J#{max_meta_size}=#{default_size}M"
  end

  compile_flags = {
    :default => :int,
    :int => ["-X-C"],
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
    mri_test_files = File.readlines('test/mri.index').grep(/^[^#]\w+/).map(&:chomp).join(' ')
    task :int do
      ENV['JRUBY_OPTS'] = "#{ENV['JRUBY_OPTS']} -Xbacktrace.style=mri -Xdebug.fullTrace -X-C"
      ruby "-r ./test/mri_test_env.rb test/mri/runner.rb #{ADDITIONAL_TEST_OPTIONS} -q -- #{mri_test_files}"
    end

    task :fullint do
      ENV['JRUBY_OPTS'] = "#{ENV['JRUBY_OPTS']} -Xbacktrace.style=mri -Xdebug.fullTrace -X-C -Xjit.threshold=0 -Xjit.background=false"
      ruby "-r ./test/mri_test_env.rb test/mri/runner.rb #{ADDITIONAL_TEST_OPTIONS} -q -- #{mri_test_files}"
    end

    task :jit do
      ENV['JRUBY_OPTS'] = "#{ENV['JRUBY_OPTS']} -Xbacktrace.style=mri -Xdebug.fullTrace -Xjit.threshold=0 -Xjit.background=false #{get_meta_size.call()}"
      ruby "-r ./test/mri_test_env.rb test/mri/runner.rb #{ADDITIONAL_TEST_OPTIONS} -q -- #{mri_test_files}"
    end

    task :aot do
      ENV['JRUBY_OPTS'] = "#{ENV['JRUBY_OPTS']} -Xbacktrace.style=mri -Xdebug.fullTrace -X+C -Xjit.background=false #{get_meta_size.call()}"
      ruby "-r ./test/mri_test_env.rb test/mri/runner.rb #{ADDITIONAL_TEST_OPTIONS} -q -- #{mri_test_files}"
    end

    task all: %s[int jit aot]
  end
  task mri: 'test:mri:int'

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
    t.verbose = true
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
    t.verbose = true
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
    t.verbose = true
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
  
  namespace :junit do
    test_class_path = [
      "target/junit.jar",
      "target/livetribe-jsr223.jar",
      "target/bsf.jar",
      "target/commons-logging.jar",
      "lib/jruby.jar",
      "target/test-classes",
      "test/jruby/requireTest.jar",
      "test"
    ]
    
    desc "Run the main JUnit test suite"
    task :main => 'test:compile' do
      junit :classpath => test_class_path, :test => "org.jruby.test.MainTestSuite", :maxmemory => '500M' do
        jvmarg :line => '-ea'
      end
    end
  end
end
