require File.dirname(__FILE__) + "/../spec_helper"
require 'tempfile'

describe java.net.URL do
  before :each do
    file = File.expand_path(__FILE__)
    @url = file =~ /^[a-z]:/i ? "file:/#{file}" : "file://#{file}"
  end

  it "should have an #open extension mechanism which yields an IO object" do
    contents = java.net.URL.new(@url).open do |io|
      io.read
    end
    expect(contents).to eq(File.read(__FILE__))
  end

  it "can used with 'open-uri' and passed to #open and yield an IO" do
    require 'open-uri'
    contents = open(java.net.URL.new(@url)) do |io|
      io.read
    end
    expect(contents).to eq(File.read(__FILE__))
  end
end
