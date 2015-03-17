require File.expand_path('../../../../../spec_helper', __FILE__)
require 'net/http'
require File.expand_path('../fixtures/http_server', __FILE__)

describe "Net::HTTP#options" do
  before(:all) do
    NetHTTPSpecs.start_server
  end

  after(:all) do
    NetHTTPSpecs.stop_server
  end

  before(:each) do
    @http = Net::HTTP.start("localhost", 3333)
  end

  after(:each) do
    @http.finish if @http.started?
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
