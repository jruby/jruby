require 'test/unit'
require 'test/test_helper'
require 'rbconfig'


def load_behavior_block(&block)
  eval("__FILE__", block.binding)
end

class TestLoad < Test::Unit::TestCase
  include TestHelper

  def test_require
    # Allow us to run MRI against non-Java dependent tests
    if RUBY_PLATFORM=~/java/
      $ruby_init = false
      file = __FILE__
      if (File::Separator == '\\')
        file.gsub!('\\\\', '/')
      end
      # Load jar file RubyInitTest.java
      require File::dirname(file) + "/RubyInitTest"
      assert($ruby_init)

      # JRUBY-1229, allow loading jar files without manifest
      assert_nothing_raised {
        require "test/jar_with_no_manifest.jar"
      }
    end

    assert require('test/requireTarget')
    assert !require('test/requireTarget')

    $loaded_foo_bar = false
    assert require('test/foo.bar')
    assert $loaded_foo_bar
  end
  
  def test_require_bogus
    assert_raises(LoadError) { require 'foo/' }
    assert_raises(LoadError) { require '' }
    
    # Yes, the following line is supposed to appear twice
    assert_raises(LoadError) { require 'NonExistantRequriedFile'}
    assert_raises(LoadError) { require 'NonExistantRequriedFile'}
  end

  def test_require_jar_should_make_its_scripts_accessible
    require 'test/jar_with_ruby_files'
    require 'hello_from_jar'
    assert "hi", $hello
  end

  def call_extern_load_foo_bar(classpath = nil)
    cmd = ""
    cmd += "env CLASSPATH=#{classpath}" # classpath=nil, becomes empty CLASSPATH
    cmd += " #{Config::CONFIG['bindir']}/#{Config::CONFIG['RUBY_INSTALL_NAME']} -e "
    cmd += "'"+'begin load "./test/foo.bar.rb"; rescue Exception => e; print "FAIL"; else print "OK"; end'+"'"
    `#{cmd}`
  end

  def test_load_relative_with_classpath
    # FIX for Windows
    unless WINDOWS
      assert_equal call_extern_load_foo_bar(File.join('test', 'jar_with_ruby_files.jar')), 'OK'
    end
  end

  def test_load_relative_with_classpath_ends_colon
    # FIX for Windows
    unless WINDOWS
      assert_equal call_extern_load_foo_bar(File.join('test', 'jar_with_ruby_files.jar') + ':'), 'OK'
    end
  end

  def test_load_relative_without_classpath
    # FIX for Windows
    unless WINDOWS
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
  
  def test_overriding_require_shouldnt_cause_problems
    eval(<<DEPS, binding, "deps")
class ::Object
  alias old_require require
  
  def require(file)
    old_require(file)
  end
end
DEPS

    require 'test/test_loading_behavior'

    res = File.expand_path($loading_behavior_result)

    assert_equal File.expand_path(File.join('test', 'test_loading_behavior.rb')), res
  end
  
  def test_loading_so_fails
    assert_raise(LoadError) { load("test/bogus.so") }
  end
  
  def test_require_relative_from_jar_in_classpath
    $CLASSPATH << File.join(
      File.dirname(__FILE__), 'jar_with_relative_require1.jar')
    require 'test/require_relative1'
  end

  def test_loading_jar_with_dot_so
    assert_nothing_raised {
      require 'test/jruby-3977.so.jar'
      load 'jruby-3977.rb'
      assert $jruby3977
    }
  end
end
