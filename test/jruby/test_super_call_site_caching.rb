require 'test/unit'

class SuperCachedCallSiteTest < Test::Unit::TestCase
  class ApplicationController
    attr_reader :path
    def initialize
      @path = []
    end

    def render
      @path << "ApplicationController"
    end
  end

  class FooController < ApplicationController
    def self.expected_path
      ["SpecialRenderMethod", "FooController", "ApplicationController"]
    end

    def render
      @path << "FooController"
      super
    end
  end

  class BarController < ApplicationController
    def self.expected_path
      ["SpecialRenderMethod", "BarController", "ApplicationController"]
    end

    def render
      @path << "BarController"
      super
    end
  end

  module SpecialRenderMethod
    def render(*args,&block)
      @path << "SpecialRenderMethod"
      super(*args,&block)
    end
  end

  module SomeOtherRenderStuff
  end

  module ExtraSpecialRenderMethod
    include SomeOtherRenderStuff
    include SpecialRenderMethod
  end

  def assert_paths(mod)
    [FooController, BarController].each do |controller_class|
      controller = controller_class.new
      (class << controller; self; end).class_eval do
        include mod
      end
      controller.render
      assert_equal controller_class.expected_path, controller.path
    end
  end

  def test_super_call_paths_without_extra_module_inserted
    assert_paths SpecialRenderMethod
  end

  def test_super_call_paths_with_extra_module_inserted
    assert_paths ExtraSpecialRenderMethod
  end
  
  # JRUBY-4568: Concurrency issue with SuperCallSite
  class Top
    def foo
      "foo"
    end

    def bar
      "bar"
    end
  end

  class Bottom < Top
    body = proc do
      super()
    end

    define_method :foo, &body
    define_method :bar, &body
  end

  def test_super_callsite_concurrency
    assert_nothing_raised do
      (1..10).to_a.map do
        Thread.new do
          10_000.times do
            begin
              Thread.main.raise unless Bottom.new.foo == "foo"
              Thread.main.raise unless Bottom.new.bar == "bar"
            rescue
              Thread.main.raise $!
            end
          end
        end
      end.each {|t| t.join}
    end
  end
end
