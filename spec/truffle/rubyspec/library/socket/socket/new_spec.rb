require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "Socket.new" do
  before :each do
    @socket = nil
  end

  after :each do
    @socket.close if @socket and not @socket.closed?
  end

  describe "with a Socket::PF_INET family" do
    it "creates a socket with a :DGRAM type specifier" do
      @socket = Socket.new Socket::PF_INET, "SOCK_DGRAM", 0
      @socket.should be_an_instance_of(Socket)
    end

    it "creates a socket with a Socket::PF_STREAM type specifier" do
      @socket = Socket.new Socket::PF_INET, "SOCK_STREAM", 0
      @socket.should be_an_instance_of(Socket)
    end
  end

  describe "with a Socket::PF_UNIX family" do
    it "creates a socket with a :DGRAM type specifier" do
      @socket = Socket.new Socket::PF_UNIX, "SOCK_DGRAM", 0
      @socket.should be_an_instance_of(Socket)
    end

    it "creates a socket with a :STREAM type specifier" do
      @socket = Socket.new Socket::PF_UNIX, "SOCK_STREAM", 0
      @socket.should be_an_instance_of(Socket)
    end
  end

  it "calls #to_str to convert the family to a String" do
    family = mock("socket new family")
    family.should_receive(:to_str).and_return("PF_UNIX")
    @socket = Socket.new family, "SOCK_STREAM", 0
    @socket.should be_an_instance_of(Socket)
  end

  it "raises a TypeError if #to_str does not return a String" do
    family = mock("socket new family")
    family.should_receive(:to_str).and_return(1)
    lambda { @socket = Socket.new family, "SOCK_STREAM", 0 }.should raise_error(TypeError)
  end

  ruby_version_is ""..."1.9" do
    it "raises an Errno::EPROTONOSUPPORT if socket type is not a string or integer" do
      lambda { Socket.new(Socket::PF_UNIX, :DGRAM, 0) }.should raise_error(Errno::EPROTONOSUPPORT)
    end
  end

  ruby_version_is "1.9" do
    it "raises a SocketError if given symbol is not a Socket constants reference" do
      lambda { Socket.new(Socket::PF_UNIX, :NO_EXIST, 0) }.should raise_error(SocketError)
    end

    it "creates a socket with an :INET family specifier" do
      @socket = Socket.new :INET, :STREAM
      @socket.should be_an_instance_of(Socket)
    end

    it "creates a socket with an 'INET' family specifier" do
      @socket = Socket.new "INET", :STREAM
      @socket.should be_an_instance_of(Socket)
    end

    it "creates a socket with a :UNIX family specifier" do
      @socket = Socket.new :UNIX, :STREAM
      @socket.should be_an_instance_of(Socket)
    end

    it "creates a socket with a 'UNIX' family specifier" do
      @socket = Socket.new "UNIX", :STREAM
      @socket.should be_an_instance_of(Socket)
    end

    describe "with an :INET family" do
      it "creates a socket with a :DGRAM type specifier" do
        @socket = Socket.new :INET, :DGRAM
        @socket.should be_an_instance_of(Socket)
      end

      it "creates a socket with a :STREAM type specifier" do
        @socket = Socket.new :INET, :STREAM
        @socket.should be_an_instance_of(Socket)
      end
    end

    describe "with a :UNIX family" do
      it "creates a socket with a :DGRAM type specifier" do
        @socket = Socket.new :UNIX, :DGRAM
        @socket.should be_an_instance_of(Socket)
      end

      it "creates a socket with a :STREAM type specifier" do
        @socket = Socket.new :UNIX, :STREAM
        @socket.should be_an_instance_of(Socket)
      end
    end

    it "accepts an optional protocol parameter" do
      @socket = Socket.new Socket::PF_UNIX, Socket::SOCK_DGRAM, 0
      @socket.should be_an_instance_of(Socket)
    end
  end
end
