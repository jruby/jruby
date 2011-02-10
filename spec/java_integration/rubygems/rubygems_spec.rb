require File.dirname(__FILE__) + "/../spec_helper"

require 'rubygems'

describe "RubyGems extensions" do
  before :each do
    @path = Gem.path
    Gem.clear_paths
  end

  after :each do
    Gem.use_paths(nil, @path)
  end

  it "should keep URLs together when splitting paths" do
    url_paths = ["file:/var/tmp",
                 "http://jruby.org",
                 "classpath:/META-INF/jruby.home",
                 "jar:file:/var/tmp/some.jar!/some/path"]
    Gem.use_paths(nil, url_paths)
    Gem.path.should include(*url_paths)
    Gem.path.should_not include("file", "http", "classpath", "jar")
  end

  it "should not create gem subdirectories on a non-file: URL" do
    Gem.ensure_gem_subdirectories("classpath:/bogus/classpath")
    File.exist?("classpath:").should be_false
    File.exist?("classpath:/bogus/classpath").should be_false
    Gem.ensure_gem_subdirectories("file:")
    File.exist?("file:").should be_false
  end
end
