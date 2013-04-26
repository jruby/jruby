# -*- encoding: US-ASCII -*-
require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "BasicSocket#recv" do

  before :each do
    @server = TCPServer.new('127.0.0.1', SocketSpecs.port)
  end

  after :each do
    @server.closed?.should be_false
    @server.close
    ScratchPad.clear
  end

  it "receives a specified number of bytes of a message from another socket"  do
    t = Thread.new do
      client = @server.accept
      ScratchPad.record client.recv(10)
      client.recv(1) # this recv is important
      client.close
    end
    Thread.pass while t.status and t.status != "sleep"
    t.status.should_not be_nil

    socket = TCPSocket.new('127.0.0.1', SocketSpecs.port)
    socket.send('hello', 0)
    socket.close

    t.join
    ScratchPad.recorded.should == 'hello'
  end

  it "accepts flags to specify unusual receiving behaviour" do
    t = Thread.new do
      client = @server.accept

      # in-band data (TCP), doesn't receive the flag.
      ScratchPad.record client.recv(10)

      # this recv is important (TODO: explain)
      client.recv(10)
      client.close
    end
    Thread.pass while t.status and t.status != "sleep"
    t.status.should_not be_nil

    socket = TCPSocket.new('127.0.0.1', SocketSpecs.port)
    socket.send('helloU', Socket::MSG_OOB)
    socket.shutdown(1)
    t.join
    socket.close
    ScratchPad.recorded.should == 'hello'
  end

  it "gets lines delimited with a custom separator"  do
    t = Thread.new do
      client = @server.accept
      ScratchPad.record client.gets("\377")

      # this call is important (TODO: explain)
      client.gets(nil)
      client.close
    end
    Thread.pass while t.status and t.status != "sleep"
    t.status.should_not be_nil

    socket = TCPSocket.new('127.0.0.1', SocketSpecs.port)
    socket.write("firstline\377secondline\377")
    socket.close

    t.join
    ScratchPad.recorded.should == "firstline\377"
  end

end
