require 'java'
require 'jruby/core_ext'
require 'junit-4.6.jar'
require 'annotator'

module JUnit
  import org.junit.Assert
  import org.junit.runner.JUnitCore
  JUnitTest = org.junit.Test

  def self.append_features(cls)
    super
    cls.extend(self)
  end

  def self.run(*classes)
    java_classes = classes.map{|c| c.become_java!; c.java_class}
    result = JUnitCore.run_classes(java_classes.to_java(java.lang.Class))
    puts "
Tests run: #{result.run_count}
Tests failed: #{result.failure_count}
Successful: #{result.was_successful?}
Elapsed time: #{result.run_time / 1000.0}s"
  end

  class TestAnno
    def initialize(parent)
      @parent = parent
    end

    def +@
      @parent.annotate_method JUnitTest
    end
  end

  def Test
    TestAnno.new(self)
  end

  def assert(*args)
    Assert.assert_true(*args)
  end
end