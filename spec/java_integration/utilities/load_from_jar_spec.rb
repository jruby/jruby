require File.dirname(__FILE__) + "/../spec_helper"
require 'java'

describe "Loading scripts from jar files" do
  it "should correctly report $LOADED_FEATURES" do
    jar = File.expand_path("test/jruby/dir with spaces/test_jar.jar")
    expect(require("#{jar}!abc/foo")).to eq(true)
    expect($LOADED_FEATURES.pop).to match(%r{dir with spaces/test_jar.jar!abc/foo.rb})

    # JRuby has supported a test_jar.jar!/abc/foo.rb alias for 'abc/foo.rb' entry in the test.jar for quite some time
    # (more correct would be test.jar!foo.rb)
    expect(require("#{jar}!/abc/foo")).to eq(true)
    expect($LOADED_FEATURES.pop).to eq("#{jar}!/abc/foo.rb")
  end

  it "blegh" do
    jar = File.expand_path("test/jruby/dir with spaces/test_jar.jar")
    load jar
    expect(File.size?("uri:classloader:/inside_jar.rb")).to eq(108)
  end

  # JRUBY-4774, WARBLER-15
  it "works with classpath URLs that have spaces in them" do
    url_loader = java.net.URLClassLoader.new([java.net.URL.new("file:" + File.expand_path("test/jruby/dir with spaces/test_jar.jar"))].to_java(java.net.URL))
    container = org.jruby.embed.ScriptingContainer.new(org.jruby.embed.LocalContextScope::SINGLETHREAD)
    container.setClassLoader(url_loader)
    expect(container.runScriptlet("begin; require 'abc/foo'; rescue LoadError; false; end")).to be_truthy
  end

  it "works when the jar path contains '#' symbols" do
    expect(require("jar:file:" + File.expand_path("test/jruby/dir with spaces/test#hash#symbol##jar.jar") + "!/abc/foo.rb")).to eq(true)
    expect($LOADED_FEATURES.pop).to match(/foo\.rb$/)

    expect(require("file:" + File.expand_path("test/jruby/dir with spaces/test#hash#symbol##jar.jar") + "!/abc/foo.rb")).to eq(true)
    expect($LOADED_FEATURES.pop).to match(/foo\.rb$/)
  end

  it "works when the load path is a jar and the path contains '#' symbols" do
    begin
      $LOAD_PATH.unshift "jar:file:" + File.expand_path("test/jruby/dir with spaces/test#hash#symbol##jar.jar") + "!/abc"

      expect(require("foo")).to eq(true)
      expect($LOADED_FEATURES.pop).to match(/foo\.rb$/)
    ensure
      $LOAD_PATH.shift
    end

    begin
      $LOAD_PATH.unshift "file:" + File.expand_path("test/jruby/dir with spaces/test#hash#symbol##jar.jar") + "!"

      expect(require("abc/foo")).to eq(true)
      expect($LOADED_FEATURES.pop).to match(/foo\.rb$/)
    ensure
      $LOAD_PATH.shift
    end
  end
end

describe "Opening files from a jar file" do
  # jruby/jruby#3399
  it "silently fails to seek on those files" do
    File.open("uri:classloader:org/jruby/kernel/kernel.rb") do |kernel_file|
      expect(kernel_file.pos).to eq(0)
      kernel_file.read(5)
      expect(kernel_file.pos).to eq(0)
      expect(kernel_file.seek(50)).to eq(0)
      expect(kernel_file.pos).to eq(0)
    end
  end
end
