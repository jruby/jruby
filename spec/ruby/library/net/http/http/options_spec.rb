require File.expand_path('../../../../../spec_helper', __FILE__)
require 'net/http'
require File.expand_path('../fixtures/http_server', __FILE__)

describe "Net::HTTP#options" do
  before :each do
    NetHTTPSpecs.start_server
    @http = Net::HTTP.start("localhost", NetHTTPSpecs.port)
  end

  after :each do
    @http.finish if @http.started?
    NetHTTPSpecs.stop_server
  end

  it "sends an options request to the passed path and returns the response" do
    response = @http.options("/request")

    ruby_version_is ''...'2.2' do
      # OPTIONS responses have no bodies
      response.body.should be_nil
    end

    ruby_version_is '2.2' do
      response.body.should == "Request type: OPTIONS"
    end
  end

  it "returns a Net::HTTPResponse" do
    @http.options("/request").should be_kind_of(Net::HTTPResponse)
  end
end
