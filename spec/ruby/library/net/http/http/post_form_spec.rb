require File.expand_path('../../../../../spec_helper', __FILE__)
require 'net/http'
require File.expand_path('../fixtures/http_server', __FILE__)

describe "Net::HTTP.post_form when passed URI" do
  before(:all) do
    NetHTTPSpecs.start_server
  end

  after(:all) do
    NetHTTPSpecs.stop_server
  end

  it "POSTs the passed form data to the given uri" do
    uri = URI.parse('http://localhost:3333/request/body')
    data = { :test => :data }

    res = Net::HTTP.post_form(uri, data)
    res.body.should == "test=data"
  end
end
