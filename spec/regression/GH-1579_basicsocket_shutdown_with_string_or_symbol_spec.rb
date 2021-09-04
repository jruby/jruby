require 'socket'

describe "BasicSocket#shutdown" do
  before(:each) do
    @server = TCPServer.new('127.0.0.1', 0)
    port = @server.addr[1]
    @client = TCPSocket.new('127.0.0.1', port)
  end

  it "accepts no arguments" do
    expect(@client.shutdown).to eql(0)
  end

  it "accepts number arguments" do
    expect(@client.shutdown(0)).to eql(0)
    expect(@client.shutdown(1)).to eql(0)
    expect(@client.shutdown(2)).to eql(0)
  end

  it "accepts string arguments" do
    expect(@client.shutdown("RD")).to eql(0)
    expect(@client.shutdown("SHUT_RD")).to eql(0)
    expect(@client.shutdown("WR")).to eql(0)
    expect(@client.shutdown("SHUT_WR")).to eql(0)
    expect(@client.shutdown("RDWR")).to eql(0)
    expect(@client.shutdown("SHUT_RDWR")).to eql(0)
  end

  it "accepts symbol arguments" do
    expect(@client.shutdown(:RD)).to eql(0)
    expect(@client.shutdown(:SHUT_RD)).to eql(0)
    expect(@client.shutdown(:WR)).to eql(0)
    expect(@client.shutdown(:SHUT_WR)).to eql(0)
    expect(@client.shutdown(:RDWR)).to eql(0)
    expect(@client.shutdown(:SHUT_RDWR)).to eql(0)
  end
end
