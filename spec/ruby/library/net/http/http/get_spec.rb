require File.expand_path('../../../../../spec_helper', __FILE__)
require 'net/http'
require File.expand_path('../fixtures/http_server', __FILE__)

describe "Net::HTTP.get when passed URI" do
  before(:all) do
    NetHTTPSpecs.start_server
  end

  after(:all) do
    NetHTTPSpecs.stop_server
  end

  describe "when passed URI" do
    it "returns the body of the specified uri" do
      Net::HTTP.get(URI.parse('http://localhost:3333/')).should == "This is the index page."
    end
  end

  describe "when passed host, path, port" do
    it "returns the body of the specified host-path-combination" do
      Net::HTTP.get('localhost', "/", 3333).should == "This is the index page."
    end
  end
end
