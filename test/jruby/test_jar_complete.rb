# gets run from 'mvn -P jruby_complete_jar_extended' which ensures that a complete jar is built first

require 'test/unit'
require 'rbconfig'
require 'fileutils'
require 'pathname'

prefix = RbConfig::CONFIG['prefix']
complete_jar = ( prefix =~ %r{^uri:classloader:.*?META-INF/jruby\.home} )

class JarCompleteTest < Test::Unit::TestCase
  include FileUtils

  # Make sure we're not affected by RVM or other gem envvars
  ENV.delete 'GEM_HOME'
  ENV.delete 'GEM_PATH'

  @@windows = RbConfig::CONFIG['host_os'] =~ /Windows|mswin/

  def file_from_url(path)
    if @@windows
      path[%r{^(:?uri:classloader):\/*?([a-zA-Z]:/[^!]+)}, 1]
    else
      path[%r{^(:?uri:classloader):\/*?(/[^!]+)}, 1]
    end
  end

  COMPLETE_JAR = ENV_JAVA["java.class.path"].split(File::PATH_SEPARATOR).find { |path| path =~ /jruby-complete-.*?\.jar/ }

  abort "error: could not figure out complete jar from 'java.class.path' (#{ENV_JAVA["java.class.path"]})" unless COMPLETE_JAR
  puts "Using jar: #{COMPLETE_JAR}"


  TMP_DIR = begin
              # Try not to use default $TMPDIR on OS X which contains +++
              tmp = [ENV['TEMP'], ENV['TMPDIR'], '/tmp', '/var/tmp'].detect {|d| d && d !~ /[ +]/ && File.directory?(d)}
              unless tmp
                require 'tempfile'
                tmp = Dir::tmpdir
              end
              Pathname.new(tmp).realpath.join("jar_complete_test").to_s
            end
  puts "Using tmp: #{TMP_DIR}"

  def setup
    mkdir_p TMP_DIR
  end

  def teardown
    rm_rf TMP_DIR
  end

  def jruby_complete(*args)
    jarfile = args.first
    if jarfile =~ /\.jar/
      args.shift
    else
      jarfile = COMPLETE_JAR
    end
    cmd = (%w(java -jar) + [%{"#{jarfile}"}] + [args].flatten).join(' ')
    output = `#{cmd}`
    puts cmd, output if $DEBUG && $?.success?
    fail "#{cmd} failed with status: #{$?.exitstatus}\n#{output}" unless $?.success?
    output
  end

  def test_complete_jar
    assert_equal "hi\n", jruby_complete("-e \"puts 'hi'\"")
  end

  def test_rubygems_home
    gem_dir = jruby_complete("-e 'puts Gem.dir'").chomp
    assert_equal 'uri:classloader:/META-INF/jruby.home/lib/ruby/gems/shared', gem_dir
  end

  def test_contains_rake_gem
    assert jruby_complete("-S gem list") =~ /rake/m
  end

  def test_rake_help_works
    output = jruby_complete("-S rake --help")
    assert output =~ /rakefile/
    assert output =~ /dry-run/
  end

  def test_prefix_in_path_with_spaces
    mkdir_p tmp = File.join(TMP_DIR, "hi there")
    complete_jar = File.expand_path(File.join(tmp, 'jruby-complete.jar'))
    cp COMPLETE_JAR, complete_jar
    output = jruby_complete(complete_jar, "-rrbconfig -e 'puts RbConfig::CONFIG[%{prefix}];'").chomp
    assert_equal 'uri:classloader://META-INF/jruby.home', output
  end

  def test_prefix_in_path_with_pluses
    mkdir_p tmp = File.join(TMP_DIR, "hi+there")
    complete_jar = File.expand_path(File.join(tmp, 'jruby-complete.jar'))
    cp COMPLETE_JAR, complete_jar
    output = jruby_complete(complete_jar, "-rrbconfig -e 'puts RbConfig::CONFIG[%{prefix}]'").chomp
    #file = file_from_url(output)
    #assert File.exists?(file), "file: #{file} (from #{output.inspect}) does not exist"
    assert_equal 'uri:classloader://META-INF/jruby.home', output
  end

  # JRUBY-5337
  def test_script_with__FILE__constant_in_jar_with_spaces
    mkdir_p tmp = File.join(TMP_DIR, "hi there")
    complete_jar = File.expand_path(File.join(tmp, 'jruby-complete.jar'))
    cp COMPLETE_JAR, complete_jar
    script = File.join(TMP_DIR, '_file_constant_.rb')
    File.open(script, 'wb') {|f| f.puts "puts __FILE__" }
    Dir.chdir(File.dirname(script)) { system %{jar uf "#{complete_jar}" #{File.basename(script)}} }
    output = jruby_complete(complete_jar, %{-e "require '_file_constant_'"}).chomp
    assert_match /uri:classloader\:\/_file_constant_\.rb/, output
  end

  def test_globing_with__dir__in_jar # GH-4611
    mkdir_p tmp = File.join(TMP_DIR, __method__.to_s)
    complete_jar = File.expand_path(File.join(tmp, 'jruby-complete.jar'))
    cp COMPLETE_JAR, complete_jar

    File.open(sample = File.join(TMP_DIR, 'sample.rb'), 'wb') do |f|
      f.puts 'puts "sample " + __dir__'
    end
    File.open(_init_ = File.join(TMP_DIR, '_init_.rb'), 'wb') do |f|
      f.puts 'puts __FILE__'
      f.puts 'puts File.dirname(__FILE__)'
      f.puts 'puts  __dir__'
      f.puts 'Dir[ "#{__dir__}/*.rb" ].each { |f| require f }'
    end
    File.open(_jruby = File.join(TMP_DIR, '.jrubydir'), 'wb') do |f|
      f.puts '.'
      f.puts '_init_.rb'
      f.puts 'sample.rb'
      f.puts ''
    end

    Dir.chdir(File.dirname(_init_)) do
      files = [sample, _init_, _jruby].map { |f| File.basename(f) }
      system %{jar uf "#{complete_jar}" #{files.join(' ')}}
    end
    output = jruby_complete(complete_jar, %{-e "require '_init_'"}).chomp

    puts output.inspect if $VERBOSE

    output = output.split("\n")

    assert_equal 'uri:classloader:/_init_.rb', output[0] # __FILE
    assert_equal 'uri:classloader:/', output[1] # File.dirname(__FILE__)
    assert_equal 'uri:classloader:/', output[2] # __dir__
    assert_equal 'sample uri:classloader:/', output[3] # sample: ...
  end

  def test_binscripts_can_be_run_from_classpath
    output = `java -cp \"#{COMPLETE_JAR}:test/jruby/dir with spaces/testgem.jar\" org.jruby.main.Main -S testgem`

    assert output == "Testing... 1.. 2.. 3..\n"
  end

  def test_relative_require_from_gem_on_classpath
    relative_require_jar = File.expand_path('samples/relative_require.jar', File.join(File.dirname(__FILE__), '../..'))

    `java -cp \"#{COMPLETE_JAR}:#{relative_require_jar}\" org.jruby.main.Main -rrelative_require -e "puts RelativeRequire::VERSION"`

    assert $? == 0, "`java -cp ... org.jruby.main.Main -rrelative_require returned: #{$?.inspect}"
  end

end if complete_jar

warn "#{__FILE__} must be launched from complete jar (found prefix = #{prefix})" unless complete_jar
