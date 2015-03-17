require File.expand_path('../../../../../spec_helper', __FILE__)
require 'net/http'
require File.expand_path('../fixtures/http_server', __FILE__)

describe "Net::HTTP.start" do
  before(:all) do
    NetHTTPSpecs.start_server
  end

  after(:all) do
    NetHTTPSpecs.stop_server
  end

  describe "when not passed a block" do
    before(:each) do
      @http = Net::HTTP.start("localhost", 3333)
    end

    after(:each) do
      @http.finish if @http.started?
    end

    it "returns a new Net::HTTP object for the passed address and port" do
      @http.should be_kind_of(Net::HTTP)
      @http.address.should == "localhost"
      @http.port.should == 3333
    end

    it "opens the tcp connection" do
      @http.started?.should be_true
    end
  end

  describe "when passed a block" do
    it "returns the blocks return value" do
      Net::HTTP.start("localhost", 3333) { :test }.should == :test
    end

    it "yields the new Net::HTTP object to the block" do
      yielded = false
      Net::HTTP.start("localhost", 3333) do |net|
        yielded = true
        net.should be_kind_of(Net::HTTP)
      end
      yielded.should be_true
    end

    it "opens the tcp connection before yielding" do
      Net::HTTP.start("localhost", 3333) { |http| http.started?.should be_true }
    end

    it "closes the tcp connection after yielding" do
      net = nil
      Net::HTTP.start("localhost", 3333) { |x| net = x }
      net.started?.should be_false
    end
  end
end

describe "Net::HTTP#start" do
  before(:all) do
    NetHTTPSpecs.start_server
  end

  after(:all) do
    NetHTTPSpecs.stop_server
  end

  before(:each) do
    @http = Net::HTTP.new("localhost", 3333)
  end

  after(:each) do
    @http.finish if @http.started?
  end

  it "returns self" do
    @http.start.should equal(@http)
  end

  it "opens the tcp connection" do
    @http.start
    @http.started?.should be_true
  end

  describe "when self has already been started" do
    it "raises an IOError" do
      @http.start
      lambda { @http.start }.should raise_error(IOError)
    end
  end

  describe "when passed a block" do
    it "returns the blocks return value" do
      @http.start { :test }.should == :test
    end

    it "yields the new Net::HTTP object to the block" do
      yielded = false
      @http.start do |http|
        yielded = true
        http.should equal(@http)
      end
      yielded.should be_true
    end

    it "opens the tcp connection before yielding" do
      @http.start { |http| http.started?.should be_true }
    end

    it "closes the tcp connection after yielding" do
      @http.start { }
      @http.started?.should be_false
    end
  end
end
