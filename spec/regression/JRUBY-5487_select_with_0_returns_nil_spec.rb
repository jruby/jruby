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

    expect(readables).not_to be_nil
    expect(readables[0]).to eq(@client_sock)
    expect(errables).to eq([])
  end

  it "selects a single writable socket" do
    readables, writables, errables = 
      Kernel.select(nil, [@client_sock], nil, 0)

    expect(writables).not_to be_nil
    expect(writables[0]).to eq(@client_sock)
  end

  it "selects a readable (data in buffer), writable socket as readable and writable" do
    readables, writables, errables = 
      Kernel.select([@client_sock], [@client_sock], [@client_sock], 0)

    expect(readables).not_to be_nil
    expect(writables).not_to be_nil
    expect(errables).not_to be_nil
    
    expect(readables[0]).to eq(@client_sock)
    expect(writables[0]).to eq(@client_sock)
  end


end
