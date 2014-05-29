require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "UNIXServer#accept_nonblock" do

  platform_is_not :windows do
    before :each do
      @path = SocketSpecs.socket_path
      rm_r @path

      @server = UNIXServer.open(@path)
      @client = UNIXSocket.open(@path)

      @socket = @server.accept_nonblock
      @client.send("foobar", 0)
    end

    after :each do
      @socket.close
      @client.close
      @server.close
      rm_r @path
    end

    it "accepts a connection in a non-blocking way" do
      data = @socket.recvfrom(6).first
      data.should == "foobar"
    end

    it "returns a UNIXSocket" do
      @socket.should be_kind_of(UNIXSocket)
    end
  end
end
