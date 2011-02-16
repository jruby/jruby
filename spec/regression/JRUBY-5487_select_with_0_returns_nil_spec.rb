require 'rspec'
require 'socket'

describe "Kernel#select with a zero timeout" do
  before :each do
    @server = TCPServer.new("localhost", 0)
    @serve_thread = Thread.new{@server.accept}
    @client_sock = TCPSocket.new("localhost", @server.addr[1])

    @serve_thread.join
    
    @server_sock = @serve_thread.value
    @server_sock.write("foo\nbar")
    _ = @client_sock.readline
  end

  after :each do
    @server_sock.close
    @client_sock.close
  end

  it "selects a single readable socket with data in buffer" do
    readables, writables, errables = 
      Kernel.select([@client_sock], nil, nil, 0)

    readables.should_not be_nil
    readables[0].should == @client_sock
    errables.should == []
  end

  it "selects a single writable socket" do
    readables, writables, errables = 
      Kernel.select(nil, [@client_sock], nil, 0)

    writables.should_not be_nil
    writables[0].should == @client_sock
  end

  it "selects a readable (data in buffer), writable socket as readable and writable" do
    readables, writables, errables = 
      Kernel.select([@client_sock], [@client_sock], [@client_sock], 0)

    readables.should_not be_nil
    writables.should_not be_nil
    errables.should_not be_nil
    
    readables[0].should == @client_sock
    writables[0].should == @client_sock
  end


end
