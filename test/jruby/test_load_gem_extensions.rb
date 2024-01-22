require 'test/unit'
require 'test/jruby/test_helper'
require 'org/jruby/kernel/path_helper'

class TestLoadGemExtensions < Test::Unit::TestCase
  include TestHelper

  def setup
    @prev_loaded_features = $LOADED_FEATURES.dup
    @prev_load_path = $LOAD_PATH.dup
  end

  def teardown
    $LOADED_FEATURES.replace(@prev_loaded_features)
    $LOAD_PATH.clear
    $LOAD_PATH.concat(@prev_load_path)
  end

  def test_require_extension_file_via_uri_protocol
    return skip 'needs jruby-home from filesystem' if inside_jar?
    require 'uri:file:./lib/ruby/stdlib/json/ext/parser'
    # just check if extension class exists
    JSON::Ext::Parser
  end

  def test_require_extension_file_via_uri_classloader_protocol
    return skip 'needs jruby-home from filesystem' if inside_jar?
    require 'uri:classloader:/lib/ruby/stdlib/json/ext/generator'
    # just check if extension class exists
    JSON::Ext::Generator
  end

  private

  def inside_jar?
    JRuby.runtime.instance_config.jruby_home =~ /META-INF/
  end

end
