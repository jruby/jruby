require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "TCPServer#gets" do
  before :each do
    @server = TCPServer.new(SocketSpecs.hostname, SocketSpecs.port)
  end

  after :each do
    @server.close
  end

  ruby_bug "#", "1.8" do
    it "raises Errno::ENOTCONN on gets" do
      lambda { @server.gets }.should raise_error(Errno::ENOTCONN)
    end
  end
end
