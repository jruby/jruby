require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "TCPSocket#setsockopt" do
  before :all do
    SocketSpecs::SpecTCPServer.start
  end

  before(:each) do
    hostname = SocketSpecs::SpecTCPServer.get.hostname
    port = SocketSpecs::SpecTCPServer.get.port
    @sock = TCPSocket.new(hostname, port)
  end

  after :each do
    @sock.close unless @sock.closed?
  end

  describe "using constants" do
    it "sets the TCP nodelay to 1" do
      @sock.setsockopt(Socket::IPPROTO_TCP, Socket::TCP_NODELAY, 1).should == 0
    end
  end

  ruby_version_is "1.9" do
    describe "using symbols" do
      it "sets the TCP nodelay to 1" do
        @sock.setsockopt(:IPPROTO_TCP, :TCP_NODELAY, 1).should == 0
      end

      context "without prefix" do
        it "sets the TCP nodelay to 1" do
          @sock.setsockopt(:TCP, :NODELAY, 1).should == 0
        end
      end
    end

    describe "using strings" do
      it "sets the TCP nodelay to 1" do
        @sock.setsockopt('IPPROTO_TCP', 'TCP_NODELAY', 1).should == 0
      end

      context "without prefix" do
        it "sets the TCP nodelay to 1" do
          @sock.setsockopt('TCP', 'NODELAY', 1).should == 0
        end
      end
    end
  end
end