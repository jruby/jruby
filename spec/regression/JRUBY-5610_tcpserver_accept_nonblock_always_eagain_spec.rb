require 'rspec'

describe "JRUBY-5610: TCPServer#accept_nonblock" do
  before :all do
    @reverse = Socket.do_not_reverse_lookup
    Socket.do_not_reverse_lookup = true
  end
  
  after :all do
    Socket.do_not_reverse_lookup = @reverse
  end
  
  it "should not raise EAGAIN if a connection is available" do
    begin
      server = TCPServer.new('127.0.0.1', 0)
      port = Socket.unpack_sockaddr_in(server.getsockname)[0]

      client = client = TCPSocket.open('127.0.0.1', port)

      lambda do
        server.accept_nonblock
      end.should_not raise_error
    ensure
      client.close if client
      server.close if server
    end
  end
end
