require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "UNIXSocket#send_io" do

  platform_is_not :windows do
    before :each do
      @path = SocketSpecs.socket_path
      File.unlink(@path) if File.exists?(@path)

      @server = UNIXServer.open(@path)
      @client = UNIXSocket.open(@path)
    end

    after :each do
      @client.close
      @server.close
      File.unlink(@path) if File.exists?(@path)
    end

    it "sends the fd for an IO object across the socket" do
      path = File.expand_path('../../fixtures/send_io.txt', __FILE__)
      f = File.open(path)

      @client.send_io(f)
      io = @server.accept.recv_io

      io.read.should == File.read(path)
    end
  end
end
