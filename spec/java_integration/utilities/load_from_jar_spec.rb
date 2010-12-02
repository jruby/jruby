require File.dirname(__FILE__) + "/../spec_helper"
require 'java'

describe "Loading scripts from jar files" do
  # JRUBY-4774, WARBLER-15
  it "works with classpath URLs that have spaces in them" do
    url_loader = java.net.URLClassLoader.new([java.net.URL.new("file:" + File.expand_path("test/dir with spaces/test_jar.jar"))].to_java(java.net.URL))
    container = org.jruby.embed.ScriptingContainer.new(org.jruby.embed.LocalContextScope::SINGLETHREAD)
    container.setClassLoader(url_loader)
    container.runScriptlet("begin; require 'abc/foo'; rescue LoadError; false; end").should be_true
  end
end
