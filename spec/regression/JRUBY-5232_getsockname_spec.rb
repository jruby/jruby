require 'socket'

describe "TCPServer#getsockname behavior: JRUBY-5232" do
  it "should return proper struct after bind call" do
    server = ::Socket.new(::Socket::AF_INET, ::Socket::SOCK_STREAM, 0)
    server.bind(::Socket.sockaddr_in(0, "127.0.0.1"))
    Socket.unpack_sockaddr_in(server.getsockname)[1].should == "127.0.0.1"
  end
end
