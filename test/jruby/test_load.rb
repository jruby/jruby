require 'test/unit'
require 'test/jruby/test_helper'
require 'rbconfig'

def load_behavior_block(&block)
  eval("__FILE__", block.binding)
end # used in test_loading_behavior

class TestLoad < Test::Unit::TestCase
  include TestHelper

  def setup
    @_LOADED_FEATURES = $LOADED_FEATURES.dup
    @_LOAD_PATH = $LOAD_PATH.dup
  end

  def teardown
    $LOADED_FEATURES.replace(@_LOADED_FEATURES)
    $LOAD_PATH.replace(@_LOAD_PATH)
  end

  def test_require
    # Allow us to run MRI against non-Java dependent tests
    if RUBY_PLATFORM=~/java/
      $ruby_init = false
      file = __FILE__
      if (File::Separator == '\\')
        file.gsub!('\\\\', '/')
      end

      # JRUBY-1229, allow loading jar files without manifest
      assert_nothing_raised {
        require "test/jruby/jar_with_no_manifest.jar"
      }
    end

    assert require('test/jruby/requireTarget')
    assert !require('test/jruby/requireTarget')

    $loaded_foo_bar = false
    assert require('test/jruby/foo.bar')
    assert $loaded_foo_bar
  end

  def test_require_without_current_dir_in_load_path
    $LOAD_PATH.delete '.'
    assert_raises(LoadError) { require('test/jruby/dummy') }
    assert require('./test/jruby/dummy')
  ensure
    $LOAD_PATH << '.'
  end

  # GH-2972
  def test_require_relative_via_uri_classloader_protocol
    $CLASSPATH << './test/jruby'
    assert_nothing_raised do
      require 'uri:classloader:/require_relative.rb'
    end
  end

  # JRUBY-3231
  def test_load_with_empty_string_in_loadpath
    begin
      $:.unshift("")
      $loaded_foo_bar = false
      assert load('test/jruby/foo.bar.rb')
      assert $loaded_foo_bar
    ensure
      $:.shift
    end
  end

  def test_LOADED_FEATURES_clear
    #_LOADED_FEATURES = $LOADED_FEATURES.dup
    begin
      require('test/jruby/foo.bar.rb')
      some_feat = $LOADED_FEATURES.first
      full_path = File.expand_path('test/jruby/foo.bar.rb')
      assert $LOADED_FEATURES.include?(full_path)

      $LOADED_FEATURES.clear
      assert_equal 0, $LOADED_FEATURES.size
      assert $LOADED_FEATURES.empty?

      assert ! $LOADED_FEATURES.include?(full_path)
      assert ! $LOADED_FEATURES.include?(some_feat)
    ensure
      #$LOADED_FEATURES.replace(_LOADED_FEATURES)
    end
  end

  def test_require_bogus
    assert_raises(LoadError) { require 'foo/' }
    assert_raises(LoadError) { require '' }

    # Yes, the following line is supposed to appear twice
    assert_raises(LoadError) { require 'NonExistantRequriedFile'}
    assert_raises(LoadError) { require 'NonExistantRequriedFile'}
  end

  def test_require_jar_should_make_its_scripts_accessible
    assert_equal "hi", run_in_sub_runtime(%{
      $hello = nil
      require 'test/jruby/jar_with_ruby_files'
      require 'hello_from_jar'
      $hello
    })
  end

  def test_require_nested_jar_should_make_its_scripts_accessible
    assert_equal "hi from nested jar", run_in_sub_runtime(%{
      $hello = nil
      require 'test/jruby/jar_with_ruby_files_in_jar'
      require 'jar_with_ruby_file'
      require 'hello_from_nested_jar'
      $hello
    })
  end

  def test_require_nested_jar_enables_class_loading_from_that_jar
    assert_in_sub_runtime %{
      require 'test/jruby/jar_with_nested_classes_jar'
      require 'jar_with_classes'
      java_import "test.HelloThere"
      HelloThere.new.message
    }
  end

  def test_require_nested_jar_defines_packages_for_classes_in_that_jar
    assert_equal "test", run_in_sub_runtime(%{
      require 'test/jruby/jar_with_nested_classes_jar'
      require 'jar_with_classes'
      java_import "test.HelloThere"
      HelloThere.java_class.package.name
    })
  end

  def call_extern_load_foo_bar(classpath = nil)
    cmd = ""
    cmd += "env CLASSPATH=#{classpath}" # classpath=nil, becomes empty CLASSPATH
    cmd += " #{RbConfig::CONFIG['bindir']}/#{RbConfig::CONFIG['RUBY_INSTALL_NAME']} -e "
    cmd += "'"+'begin load "./test/jruby/foo.bar.rb"; rescue Exception => e; print "FAIL"; else print "OK"; end'+"'"
    `#{cmd}`
  end

  unless WINDOWS || true # FIXME for Windows and 1.9+
    def test_load_relative_with_classpath
      assert_equal call_extern_load_foo_bar(File.join('test', 'jruby', 'jar_with_ruby_files.jar')), 'OK'
    end

    def test_load_relative_with_classpath_ends_colon
      assert_equal call_extern_load_foo_bar(File.join('test', 'jruby', 'jar_with_ruby_files.jar') + ':'), 'OK'
    end

    def test_load_relative_without_classpath
      assert_equal 'OK', call_extern_load_foo_bar()
    end
  end

  def test_require_with_non_existent_jar_1
    $:.unshift "file:/someHopefullyUnexistentJarFile.jar/"

    filename = File.join(File.dirname(__FILE__), "blargus1.rb")
    require_name = File.join(File.dirname(__FILE__), "blargus1")

    assert !defined?($_blargus_has_been_loaded_oh_yeah_baby_1)

    File.open(filename, "w") do |f|
      f.write <<OUT
$_blargus_has_been_loaded_oh_yeah_baby_1 = true
OUT
    end

    require require_name

    assert $_blargus_has_been_loaded_oh_yeah_baby_1
  ensure
    File.unlink(filename) rescue nil
    $:.shift
  end

  def test_require_with_non_existent_jar_2
    $:.unshift "file:/someHopefullyUnexistentJarFile.jar"

    filename = File.join(File.dirname(__FILE__), "blargus2.rb")
    require_name = File.join(File.dirname(__FILE__), "blargus2")

    assert !defined?($_blargus_has_been_loaded_oh_yeah_baby_2)

    File.open(filename, "w") do |f|
      f.write <<OUT
$_blargus_has_been_loaded_oh_yeah_baby_2 = true
OUT
    end

    require require_name

    assert $_blargus_has_been_loaded_oh_yeah_baby_2
  ensure
    File.unlink(filename) rescue nil
    $:.shift
  end

  def test_require_with_non_existent_jar_3
    $:.unshift "file:/someHopefullyUnexistentJarFile.jar!/dir/inside/that/doesnt/work"

    filename = File.join(File.dirname(__FILE__), "blargus3.rb")
    require_name = File.join(File.dirname(__FILE__), "blargus3")

    assert !defined?($_blargus_has_been_loaded_oh_yeah_baby_3)

    File.open(filename, "w") do |f|
      f.write <<OUT
$_blargus_has_been_loaded_oh_yeah_baby_3 = true
OUT
    end

    require require_name

    assert $_blargus_has_been_loaded_oh_yeah_baby_3
  ensure
    File.unlink(filename) rescue nil
    $:.shift
  end

  def test_load_rb_if_jar_doesnt_exist
    # commit f5d1f9b99aa667c4449241b43dd4dcec258548cb removed this feature
    # keep this for easier merging with jruby-1_7
    #require 'test/jruby/fake.jar' # test/fake.jar does not exist, but test/fake.jar.rb does.
  end

  def test_overriding_require_shouldnt_cause_problems
    eval(<<DEPS, binding, "deps")
class ::Object
  alias old_require require

  def require(file)
    old_require(file)
  end
end
DEPS

    require 'test/jruby/test_loading_behavior'

    res = File.expand_path($loading_behavior_result)

    assert_equal File.expand_path(File.join('test', 'jruby', 'test_loading_behavior.rb')), res
  ensure
    Object.class_eval { alias require old_require; undef :old_require }
  end

  # JRUBY-3894
  def test_require_relative_from_jar_in_classpath
    assert_in_sub_runtime %{
      $CLASSPATH << File.join(
        File.dirname('#{__FILE__}'), 'jar_with_relative_require1.jar')
      require 'test/require_relative1'
      $loaded_relative_foo
    }
  end

  # JRUBY-4875
  def test_require_relative_from_jar_in_classpath_with_different_cwd
    assert_in_sub_runtime %{
      Dir.chdir("test") do
        $CLASSPATH << File.join(File.dirname('#{__FILE__}'), 'jar_with_relative_require1.jar')
        require 'test/require_relative1'
        $loaded_relative_foo
      end
    }
  end

  # JRUBY-6172
  def test_load_from_jar_with_symlink_in_path
    if !WINDOWS
      begin
        Dir.mkdir 'not_A' unless File.exists? 'not_A'
        File.symlink("not_A", "A") unless File.symlink?('A')
        with_jruby_shell_spawning do
          `bin/jruby -e "load File.join('file:', File.join(File.expand_path(File.dirname(File.dirname('#{__FILE__}'))), 'jruby/requireTest-1.0.jar!'), 'A', 'B.rb') ; B"`
          assert_equal 0, $?
        end
      ensure
        File.delete("A") if File.symlink?('A')
        Dir.rmdir 'not_A' if File.exists? 'not_A'
      end
    end
  end

  def test_loading_jar_with_dot_so
    assert_in_sub_runtime %{
      require 'test/jruby/jruby-3977.so.jar'
      load 'jruby-3977.rb'
      $jruby3977
    }
  end

  def test_loading_jar_with_leading_underscore
    assert_in_sub_runtime %{
      require 'test/jruby/_leading_and_consecutive__underscores.jar'
      load 'test/jruby/_leading_and_consecutive__underscores.jar'
      true
    }
  end

  def test_jar_with_plus_in_name
    assert_in_sub_runtime %{
       require 'test/jruby/jar_with+.jar'
      Dir["#{File.dirname( __FILE__ )}/jar_with+.jar!/*"].size == 2
    }
  end

  # JRUBY-5045
  def test_cwd_plus_dotdot_jar_loading
    assert_equal "hi", run_in_sub_runtime(%{
      $hello = nil
      require './test/../test/jruby/jar_with_ruby_files'
      require 'hello_from_jar'
      $hello
    })
  end

  def test_symlinked_jar
    Dir.chdir('test/jruby') do
      FileUtils.cp 'jar_with_ruby_files.jar', 'jarwithoutextension' unless File.exists?('jarwithoutextension')
      File.symlink 'jarwithoutextension', 'symlink.jar' unless File.symlink?('symlink.jar')
    end

    assert_in_sub_runtime %{
      require 'test/jruby/symlink.jar'
    }
  ensure
    Dir.chdir('test/jruby') do
      [ 'jarwithoutextension', 'symlink.jar' ].each do |file|
        File.delete(file)
      end
    end
  end

  def test_load_wrapped
    load(File.expand_path('hello_dummy.rb', File.dirname(__FILE__)), true)
    assert !defined?(::Hello)
    assert !defined?(::Dummy)
  end

end

# This test is for JRUBY-3029, testing that each path is searched for .class and .rb before
# proceeding to the next path. The .class source loaded has different code from the .rb
# version, but to make them load and be testable in a specific way. I include the source
# of the .class version here...

=begin
$jruby3029 = 'class'
=end

TestLoad.class_eval do
  def test_load_class_before_rb
    $LOAD_PATH << File.expand_path('../dir1', File.dirname(__FILE__))
    $LOAD_PATH << File.expand_path('../dir2', File.dirname(__FILE__))

    require 'target'
    assert_equal 'rb', $jruby3029
  end
end