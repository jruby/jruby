require File.dirname(__FILE__) + "/../spec_helper"
require 'tempfile'

describe java.net.URL do
  it "should have an #open extension mechanism which yields an IO object" do
    contents = java.net.URL.new("file://#{File.expand_path(__FILE__)}").open do |io|
      io.read
    end
    contents.should == File.read(__FILE__)
  end

  it "can used with 'open-uri' and passed to #open and yield an IO" do
    require 'open-uri'
    contents = open(java.net.URL.new("file://#{File.expand_path(__FILE__)}")) do |io|
      io.read
    end
    contents.should == File.read(__FILE__)
  end
end
