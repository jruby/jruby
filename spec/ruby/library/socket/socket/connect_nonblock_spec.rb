require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

require 'socket'

describe "Socket#connect_nonblock" do
  before :each do
    @hostname = "127.0.0.1"
    @addr = Socket.sockaddr_in(SocketSpecs.port, @hostname)
    @socket = Socket.new(Socket::AF_INET, Socket::SOCK_STREAM, 0)
    @thread = nil
  end

  after :each do
    @socket.close
    @thread.join if @thread
  end

  it "connects the socket to the remote side" do
    ready = false
    @thread = Thread.new do
      server = TCPServer.new(@hostname, SocketSpecs.port)
      ready = true
      conn = server.accept
      conn << "hello!"
      conn.close
      server.close
    end

    Thread.pass while (@thread.status and @thread.status != 'sleep') or !ready

    begin
      @socket.connect_nonblock(@addr)
    rescue Errno::EINPROGRESS
    end

    IO.select nil, [@socket]

    begin
      @socket.connect_nonblock(@addr)
    rescue Errno::EISCONN
      # Not all OS's use this errno, so we trap and ignore it
    end

    @socket.read(6).should == "hello!"
  end

  platform_is_not :freebsd do
    it "raises Errno::EINPROGRESS when the connect would block" do
      lambda do
        @socket.connect_nonblock(@addr)
      end.should raise_error(Errno::EINPROGRESS)
    end

    ruby_version_is "1.9.2" do
      it "raises Errno::EINPROGRESS with IO::WaitWritable mixed in when the connect would block" do
        lambda do
          @socket.connect_nonblock(@addr)
        end.should raise_error(IO::WaitWritable)
      end
    end
  end
end
