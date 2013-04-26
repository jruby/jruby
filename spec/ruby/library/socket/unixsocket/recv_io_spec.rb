require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "UNIXSocket#recv_io" do

  platform_is_not :windows do
    before :each do
      @path = SocketSpecs.socket_path
      rm_r @path

      @server = UNIXServer.open(@path)
      @client = UNIXSocket.open(@path)
    end

    after :each do
      @client.close
      @server.close
      rm_r @path
    end

    it "reads an IO object across the socket" do
      path = File.expand_path('../../fixtures/send_io.txt', __FILE__)
      f = File.open(path)

      @client.send_io(f)
      io = @server.accept.recv_io

      io.read.should == File.read(path)
    end

    it "takes an optional class to use" do
      path = File.expand_path('../../fixtures/send_io.txt', __FILE__)
      f = File.open(path)

      @client.send_io(f)
      io = @server.accept.recv_io(File)

      io.should be_kind_of(File)
    end
  end
end
