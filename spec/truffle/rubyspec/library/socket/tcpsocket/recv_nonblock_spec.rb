require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "TCPSocket#recv_nonblock" do
  before :all do
    SocketSpecs::SpecTCPServer.start
  end

  before :each do
    @hostname = SocketSpecs::SpecTCPServer.get.hostname
    @socket = nil
  end

  after :each do
    if @socket
      @socket.write "QUIT"
      @socket.close
    end
  end

  it "returns a String read from the socket" do
    @socket = TCPSocket.new @hostname, SocketSpecs.port
    @socket.write "TCPSocket#recv_nonblock"

    # Wait for the server to echo. This spec is testing the return
    # value, not the non-blocking behavior.
    #
    # TODO: Figure out a good way to test non-blocking.
    IO.select([@socket])
    @socket.recv_nonblock(50).should == "TCPSocket#recv_nonblock"
  end
end
