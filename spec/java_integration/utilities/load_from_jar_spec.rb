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

  it "works when the jar path contains '#' symbols" do
    require("jar:file:" + File.expand_path("test/dir with spaces/test#hash#symbol##jar.jar") + "!/abc/foo.rb").should == true
    $LOADED_FEATURES.pop.should =~ /foo\.rb$/

    require("file:" + File.expand_path("test/dir with spaces/test#hash#symbol##jar.jar") + "!/abc/foo.rb").should == true
    $LOADED_FEATURES.pop.should =~ /foo\.rb$/
  end

  it "works when the load path is a jar and the path contains '#' symbols" do
    begin
      $LOAD_PATH.unshift "jar:file:" + File.expand_path("test/dir with spaces/test#hash#symbol##jar.jar") + "!/abc"

      require("foo").should == true
      $LOADED_FEATURES.pop.should =~ /foo\.rb$/
    ensure
      $LOAD_PATH.shift
    end

    begin
      $LOAD_PATH.unshift "file:" + File.expand_path("test/dir with spaces/test#hash#symbol##jar.jar") + "!"

      require("abc/foo").should == true
      $LOADED_FEATURES.pop.should =~ /foo\.rb$/
    ensure
      $LOAD_PATH.shift
    end
  end
end
