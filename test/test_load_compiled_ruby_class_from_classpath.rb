require "test/unit"
require "fileutils"

# Necessary because of http://jira.codehaus.org/browse/JRUBY-1579
require "jruby"
JRuby.runtime.instance_config.run_ruby_in_process = false

class LoadCompiledRubyClassFromClasspathTest < Test::Unit::TestCase
  RubyName = "runner"
  RubySource = "#{RubyName}.rb"
  RubyClass = "#{RubyName}.class"
  JarFile = "#{RubyName}_in_a_jar.jar"
  StarterName = "Starter"
  StarterSource = "#{StarterName}.java"
  StarterClass = "#{StarterName}.class"
  Manifest = "manifest.txt"

  def setup
    remove_test_artifacts
    @original_classpath = ENV["CLASSPATH"]

    # This line means we assume the test is running from the jruby root directory
    @jruby_home = Dir.pwd
  end

  def teardown
    remove_test_artifacts
    ENV["CLASSPATH"] = @original_classpath
  end

  def test_loading_compiled_ruby_class_from_classpath
    create_compiled_class

    append_to_classpath @jruby_home
    result = nil
    FileUtils.cd("..") do
      result = `#{jruby} -r#{RubyName} -e ""`
    end
    assert_equal 0, $?.exitstatus, "did not get 0 for exit status from running jruby against the class"
    assert_equal "hello from runner", result, "wrong text from runner"
  end

  def test_loading_compiled_ruby_class_from_jar
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

    `javac -cp #{jruby_jar} #{StarterSource}`
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
    `#{jruby} #{@jruby_home}/bin/jrubyc -p "" #{RubySource}`
    assert_equal 0, $?.exitstatus, "jrubyc failed to compile #{RubySource} into #{RubyClass}"
  end

  def jruby
    "#{@jruby_home}/bin/jruby"
  end

  def jruby_jar
    "#{@jruby_home}/lib/jruby.jar"
  end

  def append_to_classpath(*paths)
    current_classpath = ENV["CLASSPATH"].nil? ? "" : ENV["CLASSPATH"]
    ENV["CLASSPATH"] = "#{current_classpath}:#{paths.join(':')}"
  end
end
