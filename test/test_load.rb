require 'test/unit'

def load_behavior_block(&block)
  eval("__FILE__", block.binding)
end

class TestLoad < Test::Unit::TestCase
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
end
