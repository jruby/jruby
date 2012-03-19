# This gets run from 'ant test-jar-complete' which ensures that a
# complete jar is built first.

# Make sure we're not affected by RVM or other gem envvars
ENV.delete 'GEM_HOME'
ENV.delete 'GEM_PATH'

require 'test/unit'
require 'rbconfig'
require 'fileutils'
require 'pathname'

prefix = RbConfig::CONFIG['prefix']
abort "error: test must be launched from complete jar (found prefix = #{prefix})" unless prefix =~ %r{^file.*!/META-INF/jruby\.home}
def file_from_url(path)
  if RbConfig::CONFIG['host_os'] =~ /Windows|mswin/
    path[%r{^file:\/*?([a-zA-Z]:/[^!]+)}, 1]
  else
    path[%r{^file:\/*?(/[^!]+)}, 1]
  end
end
COMPLETE_JAR = file_from_url prefix
abort "error: could not figure out complete jar from RbConfig::CONFIG['prefix'] (#{prefix})" unless COMPLETE_JAR

puts "Using jar: #{COMPLETE_JAR}"

class JarCompleteTest < Test::Unit::TestCase
  include FileUtils
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
    assert jruby_complete("-rubygems -e 'puts Gem.dir'").chomp =~ /#{COMPLETE_JAR}/
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
    tmp = File.join(TMP_DIR, "hi there")
    mkdir_p tmp
    complete_jar = File.expand_path(File.join(tmp, 'jruby-complete.jar'))
    cp COMPLETE_JAR, complete_jar
    output = jruby_complete(complete_jar, "-rrbconfig -e 'puts RbConfig::CONFIG[%{prefix}]'").chomp
    file = file_from_url(output)
    assert File.exists?(file)
    assert_equal complete_jar, file
  end

  def test_prefix_in_path_with_pluses
    tmp = File.join(TMP_DIR, "hi+there")
    mkdir_p tmp
    complete_jar = File.expand_path(File.join(tmp, 'jruby-complete.jar'))
    cp COMPLETE_JAR, complete_jar
    output = jruby_complete(complete_jar, "-rrbconfig -e 'puts RbConfig::CONFIG[%{prefix}]'").chomp
    file = file_from_url(output)
    assert File.exists?(file)
    assert_equal complete_jar, file
  end

  # JRUBY-5337
  def test_script_with__FILE__constant_in_jar_with_spaces
    tmp = File.join(TMP_DIR, "hi there")
    mkdir_p tmp
    complete_jar = File.expand_path(File.join(tmp, 'jruby-complete.jar'))
    cp COMPLETE_JAR, complete_jar
    script = File.join(TMP_DIR, "_file_constant_.rb")
    File.open(script, "wb") {|f| f.puts "puts __FILE__" }
    Dir.chdir(File.dirname(script)) { system %{jar uf "#{complete_jar}" #{File.basename(script)}} }
    output = jruby_complete(complete_jar, %{-e "require '_file_constant_'"}).chomp
    assert output =~ /#{tmp}/, "'#{output}' does not match '#{tmp}'"
  end

  def test_binscripts_can_be_run_from_classpath
    output = `java -cp \"#{COMPLETE_JAR}:test/dir with spaces/testgem.jar\" org.jruby.Main -S testgem`

    assert output == "Testing... 1.. 2.. 3..\n"
  end
end
