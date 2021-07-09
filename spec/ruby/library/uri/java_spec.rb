require File.expand_path('../../../spec_helper', __FILE__)
require 'uri'

describe "the URI method" do
  it "parses a given URI, converts it to a java.net.URI, and back" do
    url = "http://ruby-lang.org"
    URI(url).to_java.to_string.should == url
  end

  it "parses a given URI and converts it to a java.net.URI" do
    url = "http://ruby-lang.org"
    URI(url).to_java.class.should == java.net.URI
  end
end
