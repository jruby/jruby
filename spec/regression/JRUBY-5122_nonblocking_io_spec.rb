require 'socket'
require 'timeout'
require 'fcntl'

describe "nonblocking IO blocking behavior: JRUBY-5122" do
  Socket.do_not_reverse_lookup = true

  # FYI: In JRuby 'should not block' means 'should not do busy loop'

  it "should not block for gets" do
    server = TCPServer.new(0)
    value = nil
    t = Thread.new {
      sock = accept(server)
      value = sock.gets
    }
    s = connect(server)
    wait_for_sleep_and_terminate(t) do
      s.write("foo\r\n")
    end
    value.should == "foo\r\n"
  end

  it "should not block for eof" do
    server = TCPServer.new(0)
    value = nil
    t = Thread.new {
      sock = accept(server)
      value = sock.eof?
    }
    s = connect(server)
    wait_for_sleep_and_terminate(t) do
      s.write("foo\r\n")
    end
    value.should == false
  end

  it "should not block for getc" do
    server = TCPServer.new(0)
    value = nil
    t = Thread.new {
      sock = accept(server)
      value = sock.getc
    }
    s = connect(server)
    wait_for_sleep_and_terminate(t) do
      s.write("f")
    end
    value.should == ?f
  end

  it "should not block for readlines" do
    server = TCPServer.new(0)
    value = nil
    t = Thread.new {
      sock = accept(server)
      value = sock.readlines
    }
    s = connect(server)
    wait_for_sleep_and_terminate(t) do
      s.write("foo\r\nbar\r\n")
      s.close
    end
    value.should == ["foo\r\n", "bar\r\n"]
  end

  it "should not block for sysread" do
    server = TCPServer.new(0)
    value = nil
    t = Thread.new {
      sock = accept(server)
      # TODO: it raises EAGAIN when it's a single thread. How can I test that?
      value = sock.sysread(2)
    }
    s = connect(server)
    wait_for_sleep_and_terminate(t) do
      t.alive?.should == true
      s.write("foo\r\n")
    end
    value.should == "fo"
  end

  it "should not block for each_byte" do
    server = TCPServer.new(0)
    value = nil
    t = Thread.new {
      sock = accept(server)
      sock.each_byte do |b|
        value = b
      end
    }
    s = connect(server)
    wait_for_sleep_and_terminate(t) do
      t.alive?.should == true
      s.write("foobar")
      s.close
    end
    value.should == 114
  end

  it "should not block for each_line" do
    server = TCPServer.new(0)
    value = nil
    t = Thread.new {
      sock = accept(server)
      sock.each_line do |line|
        value = line
      end
    }
    s = connect(server)
    wait_for_sleep_and_terminate(t) do
      t.alive?.should == true
      s.write("foo\r\nbar\r\nbaz")
      s.close
    end
    value.should == "baz"
  end

  BIG_CHUNK = "a" * 100_000_000
  it "should not block for write" do
    server = TCPServer.new(0)
    value = nil
    t = Thread.new {
      sock = accept(server)
      begin
        sock.write(BIG_CHUNK) # this blocks; [ruby-dev:26405]
      rescue
        value = true
      end
    }
    s = connect(server)
    wait_for_sleep_and_terminate(t) do
      t.raise # help thread termination
    end
    value.should == true
  end

  def accept(server)
    sock = server.accept
    flag = File::NONBLOCK
    flag |= sock.fcntl(Fcntl::F_GETFL)
    sock.fcntl(Fcntl::F_SETFL, flag)
    sock
  end

  def connect(server)
    TCPSocket.new('localhost', server.addr[1])
  end

  def wait_for_sleep_and_terminate(server_thread)
    wait_for_sleep(server_thread)
    yield
    wait_for_terminate(server_thread)
  end

  def wait_for_sleep(t)
    timeout(100) do
      sleep 0.1 while t.status == 'run'
    end
  end

  def wait_for_terminate(t)
    timeout(100) do
      sleep 0.1 while t.alive?
    end
  end
end
