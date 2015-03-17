describe :net_ftp_request_post, :shared => true do
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

  describe "when passed no block" do
    it "sends a post request to the passed path and returns the response" do
      response = @http.send(@method, "/request", "test=test")
      response.body.should == "Request type: POST"
    end

    it "returns a Net::HTTPResponse object" do
      response = @http.send(@method, "/request", "test=test")
      response.should be_kind_of(Net::HTTPResponse)
    end
  end

  describe "when passed a block" do
    it "sends a post request to the passed path and returns the response" do
      response = @http.send(@method, "/request", "test=test") {}
      response.body.should == "Request type: POST"
    end

    it "yields the response to the passed block" do
      @http.send(@method, "/request", "test=test") do |response|
        response.body.should == "Request type: POST"
      end
    end

    it "returns a Net::HTTPResponse object" do
      response = @http.send(@method, "/request", "test=test") {}
      response.should be_kind_of(Net::HTTPResponse)
    end
  end
end
