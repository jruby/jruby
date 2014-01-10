require File.expand_path('../../../../../spec_helper', __FILE__)
require 'net/http'
require File.expand_path('../fixtures/classes', __FILE__)

describe "Net::HTTPHeader#initialize_http_header when passed Hash" do
  before(:each) do
    @headers = NetHTTPHeaderSpecs::Example.allocate
  end

  it "initializes the HTTP Header using the passed Hash" do
    @headers.initialize_http_header("My-Header" => "test", "My-Other-Header" => "another test")
    @headers["My-Header"].should == "test"
    @headers["My-Other-Header"].should == "another test"
  end

  # TODO: Doesn't work, but works in IRB. No idea what's up here.
  #
  # it "complains about duplicate keys when in verbose mode" do
  #   old_verbose, $VERBOSE = $VERBOSE, true
  #
  #   begin
  #     lambda do
  #       @headers.initialize_http_header("My-Header" => "test", "my-header" => "another test")
  #     end.should complain("net/http: warning: duplicated HTTP header: My-Header\n")
  #   ensure
  #     $VERBOSE = old_verbose
  #   end
  # end
end
