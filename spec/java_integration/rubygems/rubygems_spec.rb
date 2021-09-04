require File.dirname(__FILE__) + "/../spec_helper"

require 'tmpdir'

describe "RubyGems extensions" do

  before(:all) { require 'rubygems' }

  before :each do
    @path = Gem.path
    Gem.clear_paths
  end

  after :each do
    Gem.use_paths(nil, @path)
  end

  it "should keep URLs together when splitting paths" do
    Dir.mktmpdir do |tmpdir|
      tmpdir = File.realpath(tmpdir)
      url_paths = ["file:#{tmpdir}",
                   "http://jruby.org",
                   "classpath:/META-INF/jruby.home",
                   "uri:classpath:/META-INF/jruby.home",
                   "uri:classpath:/",
                   "uri:jar:file://META-INF/jruby.home!/some/path",
                   "jar:file:#{tmpdir}/some.jar!/some/path"]
      Gem.use_paths(nil, url_paths)
      expect(Gem.path).to include(*url_paths)
      expect(Gem.path).not_to include("file", "http", "classpath", "jar", "uri", "classloader")
    end
  end

  it "should not create gem subdirectories on a non-file: URL" do
    Gem.ensure_gem_subdirectories("classpath:/bogus/classpath")
    expect(File.exist?("classpath:/bogus/classpath")).to be_falsey
    Gem.ensure_gem_subdirectories("file:")
    expect(File.exist?("file:")).to be_falsey
    Gem.ensure_gem_subdirectories("uri:file://bogus/classpath")
    expect(File.exist?("uri:file:///nothing")).to be_falsey
    expect(File.exist?("uri:file://bogus/classpath")).to be_falsey
  end
end
