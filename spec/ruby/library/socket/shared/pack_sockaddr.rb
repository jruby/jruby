describe :socket_pack_sockaddr_in, :shared => true do
  it "packs and unpacks" do
    sockaddr_in = Socket.pack_sockaddr_in 0, nil
    port, addr = Socket.unpack_sockaddr_in(sockaddr_in)
    ["127.0.0.1", "::1"].include?(addr).should == true
    port.should == 0

    sockaddr_in = Socket.pack_sockaddr_in 0, ''
    Socket.unpack_sockaddr_in(sockaddr_in).should == [0, '0.0.0.0']

    sockaddr_in = Socket.pack_sockaddr_in 80, '127.0.0.1'
    Socket.unpack_sockaddr_in(sockaddr_in).should == [80, '127.0.0.1']

    sockaddr_in = Socket.pack_sockaddr_in '80', '127.0.0.1'
    Socket.unpack_sockaddr_in(sockaddr_in).should == [80, '127.0.0.1']
  end
end

describe :socket_pack_sockaddr_un, :shared => true do
  not_supported_on :jruby, :windows do
    it "packs and unpacks" do
      sockaddr_un = Socket.pack_sockaddr_un '/tmp/s'
      Socket.unpack_sockaddr_un(sockaddr_un).should == '/tmp/s'
    end
  end
end
