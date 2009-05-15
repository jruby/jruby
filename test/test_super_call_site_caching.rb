require 'test/unit'

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

class SuperCachedCallSiteTest < Test::Unit::TestCase
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
end
