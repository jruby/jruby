describe :socket_socketpair, :shared => true do
  not_supported_on :jruby, :windows do
    it "ensures the returned sockets are connected" do
      s1, s2 = Socket.socketpair(Socket::AF_UNIX, 1, 0)
      s1.puts("test")
      s2.gets.should == "test\n"
      s1.close
      s2.close
    end
  end

  it "only allows Socket constants as strings" do
    [ :DGRAM, :RAW, :RDM, :SEQPACKET, :STREAM ].each do |socket_type|
      lambda { Socket.socketpair(Socket::AF_UNIX, "SOCK_#{socket_type}", 0) }.should_not raise_error(SocketError)
    end
  end

  ruby_version_is "1.9" do
    it "raises SocketError if given symbol is not a Socket constants reference" do
      lambda { Socket.socketpair(Socket::AF_UNIX, :NO_EXIST, 0) }.should raise_error(SocketError)
    end

    it "only allows Socket constants as symbols" do
      [ :DGRAM, :RAW, :RDM, :SEQPACKET, :STREAM ].each do |socket_type|
        lambda { Socket.socketpair(Socket::AF_UNIX, socket_type, 0) }.should_not raise_error(SocketError)
      end
    end
  end

  ruby_version_is ""..."1.9" do
    it "raises Errno::EPROTONOSUPPORT if socket type is not a String or Integer" do
      lambda { Socket.socketpair(Socket::AF_UNIX, :DGRAM, 0) }.should raise_error(Errno::EPROTONOSUPPORT)
    end
  end
end
