require File.expand_path('../../../../../spec_helper', __FILE__)
require 'net/http'
require File.expand_path('../fixtures/http_server', __FILE__)

describe "Net::HTTP#post" do
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

  it "sends an post request to the passed path and returns the response" do
    response = @http.post("/request", "test=test")
    response.body.should == "Request type: POST"
  end

  it "returns a Net::HTTPResponse" do
    @http.post("/request", "test=test").should be_kind_of(Net::HTTPResponse)
  end

  describe "when passed a block" do
    it "yields fragments of the response body to the passed block" do
      str = ""
      @http.post("/request", "test=test") do |res|
        str << res
      end
      str.should == "Request type: POST"
    end

    it "returns a Net::HTTPResponse" do
      @http.post("/request", "test=test") {}.should be_kind_of(Net::HTTPResponse)
    end
  end
end
