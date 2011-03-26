require "test/unit"
require "fileutils"
require 'test/test_helper'
require 'rbconfig'

# Necessary because of http://jira.codehaus.org/browse/JRUBY-1579
require "jruby"

require 'jruby/compiler'

class LoadCompiledRubyClassFromClasspathTest < Test::Unit::TestCase
  include TestHelper

  RubyName = "runner"
  RubySource = "#{RubyName}.rb"
  RubyClass = "#{RubyName}.class"
  JarFile = "#{RubyName}_in_a_jar.jar"
  StarterName = "Starter"
  StarterSource = "#{StarterName}.java"
  StarterClass = "#{StarterName}.class"
  Manifest = "manifest.txt"

  if java.lang.System.getProperty("basedir") # FIXME: disabling this test under maven
    def test_truth; end
  else
  def setup
    remove_test_artifacts
    @original_classpath = ENV["CLASSPATH"]

    # This line means we assume the test is running from the jruby root directory
    @jruby_home = Dir.pwd
    @in_process = JRuby.runtime.instance_config.run_ruby_in_process
    JRuby.runtime.instance_config.run_ruby_in_process = false
  end

  def teardown
    remove_test_artifacts
    ENV["CLASSPATH"] = @original_classpath
    JRuby.runtime.instance_config.run_ruby_in_process = @in_process
  end

  def test_loading_compiled_ruby_class_from_classpath
    create_compiled_class

    append_to_classpath @jruby_home
    result = nil

    FileUtils.cd("..") do
      result = jruby("-r#{RubyName} -e '1'")
    end
    assert_equal 0, $?.exitstatus, "did not get 0 for exit status from running jruby against the class"
    assert_equal "hello from runner", result, "wrong text from runner"
  end

  def test_loading_compiled_ruby_class_from_jar
    return if RbConfig::CONFIG['host_os'] == "SunOS"
    create_compiled_class

    append_to_classpath jruby_jar
    File.open(StarterSource, "w") do |f|
      f.puts <<-EOS
public class #{StarterName} {
  public static void main(String args[]) throws Exception {
     org.jruby.Main.main(new String[] { "-r#{RubyName}", "-e", "" });
  }
}
      EOS
    end
    File.open(Manifest, "w") do |f|
      f.puts "Main-Class: #{StarterName}"
      f.puts "Class-Path: #{jruby_jar}"
    end

    javac = ENV['JAVA_HOME'] ? "#{ENV['JAVA_HOME']}/bin/javac" : "javac"

    `#{javac} -cp #{jruby_jar} #{StarterSource}`
    assert_equal 0, $?.exitstatus, "javac failed to compile #{StarterSource}"
    `jar cvfm #{JarFile} #{Manifest} #{StarterClass} #{RubyClass}`
    assert_equal 0, $?.exitstatus, "jar failed to build #{JarFile} from #{RubyClass}"

    remove_ruby_source_files

    result = `java -jar #{JarFile}`
    assert_equal 0, $?.exitstatus, "did not get 0 for exit status from running java against the jar"
    assert_equal "hello from runner", result, "wrong text from runner"
  end

  private
  def remove_ruby_source_files
    FileUtils.rm_rf [RubySource, RubyClass]
  end

  def remove_test_artifacts
    remove_ruby_source_files
    FileUtils.rm_rf [JarFile, StarterSource, StarterClass, Manifest]
  end

  def create_compiled_class
    File.open(RubySource, "w") { |f| f << "print 'hello from runner'" }
    JRuby::Compiler::compile_argv([RubySource])
  rescue Exception => e
    raise "jrubyc failed to compile #{RubySource} into #{RubyClass}:\n#{e.message}\n#{e.backtrace}"
  ensure
    # just in case, remove the rb file
    FileUtils.rm_rf RubySource
  end

  def jruby_jar
    "lib/jruby.jar"
  end

  def append_to_classpath(*paths)
    current_classpath = ENV["CLASSPATH"].nil? ? "" : ENV["CLASSPATH"]
    ENV["CLASSPATH"] = "#{current_classpath}#{File::PATH_SEPARATOR}#{paths.join(File::PATH_SEPARATOR)}"
  end
  end # end FIXME
end
