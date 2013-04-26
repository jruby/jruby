require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "BasicSocket#getsockopt" do
  before(:each) do
    @sock = Socket.new(Socket::AF_INET, Socket::SOCK_STREAM, 0)
  end

  after :each do
    @sock.closed?.should be_false
    @sock.close
  end

  it "gets a socket option Socket::SO_TYPE" do
    n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_TYPE).to_s
    n.should == [Socket::SOCK_STREAM].pack("i")
  end

  it "gets a socket option Socket::SO_OOBINLINE" do
    n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_OOBINLINE).to_s
    n.should == [0].pack("i")
  end

  it "gets a socket option Socket::SO_LINGER" do
    n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_LINGER).to_s
    if (n.size == 8) # linger struct on some platforms, not just a value
      n.should == [0, 0].pack("ii")
    else
      n.should == [0].pack("i")
    end
  end

  it "gets a socket option Socket::SO_SNDBUF" do
    n = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_SNDBUF).to_s
    n.unpack('i')[0].should > 0
  end

  it "raises a SystemCallError with an invalid socket option" do
    lambda { @sock.getsockopt Socket::SOL_SOCKET, -1 }.should raise_error(Errno::ENOPROTOOPT)
  end

  ruby_version_is "1.9.2" do
    it "gets a Socket::Option" do
      so = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_TYPE)
      so.class.should == Socket::Option
      so.level.should == Socket::SOL_SOCKET
      so.optname.should == Socket::SO_TYPE
      so.int.should == Socket::SOCK_STREAM

      so = @sock.getsockopt(Socket::SOL_SOCKET, Socket::SO_LINGER)
      so.optname.should == Socket::SO_LINGER
      so.linger.should == [false, 0]
    end
  end
end
