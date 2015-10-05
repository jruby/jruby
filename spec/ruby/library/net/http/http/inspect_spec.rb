require File.expand_path('../../../../../spec_helper', __FILE__)
require 'net/http'
require File.expand_path('../fixtures/http_server', __FILE__)

describe "Net::HTTP#inspect" do
  before :each do
    NetHTTPSpecs.start_server
    @http = Net::HTTP.new("localhost", 3333)
  end

  after :each do
    @http.finish if @http.started?
    NetHTTPSpecs.stop_server
  end

  it "returns a String representation of self" do
    @http.inspect.should be_kind_of(String)
    @http.inspect.should == "#<Net::HTTP localhost:3333 open=false>"

    @http.start
    @http.inspect.should == "#<Net::HTTP localhost:3333 open=true>"
  end
end
