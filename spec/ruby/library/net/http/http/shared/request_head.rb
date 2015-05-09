describe :net_ftp_request_head, :shared => true do
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
    it "sends a head request to the passed path and returns the response" do
      response = @http.send(@method, "/request")
      response.body.should be_nil
    end

    it "returns a Net::HTTPResponse object" do
      response = @http.send(@method, "/request")
      response.should be_kind_of(Net::HTTPResponse)
    end
  end

  describe "when passed a block" do
    it "sends a head request to the passed path and returns the response" do
      response = @http.send(@method, "/request") {}
      response.body.should be_nil
    end

    it "yields the response to the passed block" do
      @http.send(@method, "/request") do |response|
        response.body.should be_nil
      end
    end

    it "returns a Net::HTTPResponse object" do
      response = @http.send(@method, "/request") {}
      response.should be_kind_of(Net::HTTPResponse)
    end
  end
end
