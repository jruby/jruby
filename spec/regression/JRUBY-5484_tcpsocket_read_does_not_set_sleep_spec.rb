require 'rspec'
require 'socket'

describe "TCPSocket method" do
  before :each do
    @serv_sock = TCPServer.new("localhost", 0)
    @serv_thread = Thread.new{ @serv_sock.accept }
    Thread.pass until @serv_thread.status == 'sleep'
    @port = @serv_sock.addr[1]
  end

  def check_status(&blk)
    @client_sock = TCPSocket.new("localhost", @port)
    @client_thread = Thread.new &blk
    sleep(0.1) # potentially too short?
    @client_thread.status.should == 'sleep'
    @client_thread.kill
  end

  it "read sets thread status to sleep" do
    check_status { @client_sock.read }
  end

  it "readline sets thread status to sleep" do
    check_status { @client_sock.readline }
  end
end
