require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "Socket::TCPServer.accept_nonblock" do
  before :each do
    @server = TCPServer.new("127.0.0.1", SocketSpecs.port)
  end

  after :each do
    @server.close unless @server.closed?
  end

  it "accepts non blocking connections" do
    @server.listen(5)
    lambda {
      @server.accept_nonblock
    }.should raise_error(IO::WaitReadable)

    c = TCPSocket.new("127.0.0.1", SocketSpecs.port)
    sleep 0.1
    s = @server.accept_nonblock

    # commenting while we get some input on the current JRuby situation
    #    port, address = Socket.unpack_sockaddr_in(s.getsockname)

    #    port.should == SocketSpecs.port
    #    address.should == "127.0.0.1"
    s.should be_kind_of(TCPSocket)

    c.close
    s.close
  end

  it "raises an IOError if the socket is closed" do
    @server.close
    lambda { @server.accept }.should raise_error(IOError)
  end
end
