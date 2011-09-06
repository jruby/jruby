###
# I'm running like this:
#
#  ruby -I lib:../../i18n/lib zomg.rb
#
require 'rubygems'
require 'abstract_controller'
require 'action_controller'
require 'action_view'
require 'action_dispatch'
require 'active_support/dependencies'
require 'action_controller/caching'
require 'action_controller/caching/sweeping'

require 'active_support/ordered_options'
require 'benchmark'

class MyTest
  class BasicController
    attr_reader :request, :config

    def initialize config
      super()
      @config = config
    end
  end

  include ActionView::Helpers::AssetTagHelper
  include ActionView::Helpers::TagHelper

  attr_reader :config, :controller

  def initialize
    @config = ActiveSupport::InheritableOptions.new(ActionController::Base.config)
    public_dir = File.expand_path("public", __FILE__)
    @config.assets_dir = public_dir
    @config.javascripts_dir = "#{public_dir}/javascripts"
    @config.stylesheets_dir = "#{public_dir}/stylesheets"

    @controller = BasicController.new @config
    ActionView::Helpers::AssetTagHelper::register_javascript_expansion :defaults => ['prototype', 'effects', 'dragdrop', 'controls', 'rails']
  end

  def one_file
    javascript_include_tag("bank")
  end

  def all
    javascript_include_tag(:all)
  end

  def with_lang
    javascript_include_tag("bank", :lang => 'vbscript')
  end

  def defaults
    javascript_include_tag(:defaults)
  end

  def recursive
    javascript_include_tag(:all, :recursive => true)
  end
end

require 'benchmark/ips'

test = MyTest.new

class Blah
  def initialize
    @a = 1
    @b = 2
    @c = 3
    @d = 4
  end
end

Benchmark.ips do |x|

  # obj = Object.new

  # x.report "obj.dup" do
    # obj.dup
  # end

  # blah = Blah.new

  # x.report "blah.dup" do
    # blah.dup
  # end

  h = { "a" => "1", "b" => "2" }

  # hc = Hash

  # x.report "replace" do
    # hc.allocate.replace(h)
  # end

  # x.report "dup" do
    # h.dup
  # end

  2.times do
    x.report "stringify" do
      h.stringify_keys
    end
  end

  2.times do
    x.report "keys" do
      h.keys
    end
  end

  # x.report('one') {
    # test.one_file
  # }

  # x.report('defaults') {
    # test.defaults
  # }
end
