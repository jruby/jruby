require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../shared/new', __FILE__)

describe "UNIXSocket.open" do
  it_behaves_like :unixsocket_new, :open

  platform_is_not :windows do
    before :each do
      @path = SocketSpecs.socket_path
      rm_r @path

      @server = UNIXServer.open(@path)
    end

    after :each do
      @server.close
      rm_r @path
    end

    it "opens a unix socket on the specified file and yields it to the block" do
      UNIXSocket.send(@method, @path) do |client|
        client.addr[0].should == "AF_UNIX"
        client.closed?.should == false
      end
    end
  end
end
