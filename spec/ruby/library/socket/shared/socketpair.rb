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
end
