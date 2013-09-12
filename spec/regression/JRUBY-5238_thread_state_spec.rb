require 'socket'
require 'timeout'

describe "Thread#status behavior while blocking IO: JRUBY-5238" do
  before(:each) do
    @server = TCPServer.new('127.0.0.1', 0)
    port = @server.addr[1]
    @client = TCPSocket.new('127.0.0.1', port)
  end

  it "should be 'sleep' while blocking gets" do
    thread = Thread.new {
      @client.gets
    }
    wait_block(thread)
    thread.status.should == 'sleep'
  end

  it "should be 'sleep' while blocking gets(rs)" do
    thread = Thread.new {
      @client.gets("foo")
    }
    wait_block(thread)
    thread.status.should == 'sleep'
  end

  it "should be 'sleep' while blocking eof?" do
    thread = Thread.new {
      @client.eof?
    }
    wait_block(thread)
    thread.status.should == 'sleep'
  end

  it "should be 'sleep' while blocking getc" do
    thread = Thread.new {
      @client.getc
    }
    wait_block(thread)
    thread.status.should == 'sleep'
  end

  it "should be 'sleep' while blocking readlines" do
    thread = Thread.new {
      @client.readlines
    }
    wait_block(thread)
    thread.status.should == 'sleep'
  end

  it "should be 'sleep' while blocking read" do
    thread = Thread.new {
      @client.read
    }
    wait_block(thread)
    thread.status.should == 'sleep'
  end

  it "should be 'sleep' while blocking read(n)" do
    thread = Thread.new {
      @client.read(1)
    }
    wait_block(thread)
    thread.status.should == 'sleep'
  end

  it "should be 'sleep' while blocking readpartial" do
    thread = Thread.new {
      @client.readpartial(1)
    }
    wait_block(thread)
    thread.status.should == 'sleep'
  end

  it "should be 'sleep' while blocking sysread" do
    thread = Thread.new {
      @client.sysread(1)
    }
    wait_block(thread)
    thread.status.should == 'sleep'
  end

  # See JRUBY-5122 spec for the reason of this value
  SOCKET_CHANNEL_MIGHT_BLOCK = "a" * (65536 * 4)

  it "should be 'sleep' while blocking write" do
    blocked = true
    thread = Thread.new {
      # it doesn't block on Windows.
      @client.write(SOCKET_CHANNEL_MIGHT_BLOCK)
      blocked = false
    }
    wait_block(thread)
    if blocked
      thread.status.should == 'sleep'
    else
      # it's OK since write is not blocked. we cannot test this behavior for such case.
      thread.status.should == false
    end
  end

  it "should be 'sleep' while blocking syswrite" do
    blocked = true
    thread = Thread.new {
      # it doesn't block on Windows.
      @client.syswrite(SOCKET_CHANNEL_MIGHT_BLOCK)
      blocked = false
    }
    wait_block(thread)
    if blocked
      thread.status.should == 'sleep'
    else
      # it's OK since write is not blocked. we cannot test this behavior for such case.
      thread.status.should == false
    end
  end

  def wait_block(thread)
    begin
      timeout(2) do
        sleep 0.1 while (thread.status != false && thread.status != 'sleep')
      end
    rescue Timeout::Error
      raise "Thread#status != 'sleep'"
    end
  end
end
