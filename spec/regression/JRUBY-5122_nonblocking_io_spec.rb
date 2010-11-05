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

  it "should not block for read" do
    server = TCPServer.new(0)
    value = nil
    t = Thread.new {
      sock = accept(server)
      value = sock.read
    }
    s = connect(server)
    wait_for_sleep_and_terminate(t) do
      s.write("foo\r\nbar\r\nbaz")
      s.close
    end
    value.should == "foo\r\nbar\r\nbaz"
  end

  it "should not block for read(n) where n is shorter than the buffer" do
    server = TCPServer.new(0)
    value = nil
    t = Thread.new {
      sock = accept(server)
      value = sock.read(2)
    }
    s = connect(server)
    wait_for_sleep_and_terminate(t) do
      t.alive?.should == true
      s.write("foo\r\n")
    end
    value.should == "fo"
  end

  it "should not block for read(n) where n is longer than the buffer" do
    server = TCPServer.new(0)
    value = nil
    t = Thread.new {
      sock = accept(server)
      value = sock.read(4)
    }
    s = connect(server)
    wait_for_sleep_and_terminate(t) do
      t.alive?.should == true
      s.write("f")
      t.alive?.should == true
      s.write("oo\r\n")
    end
    value.should == "foo\r"
  end

  it "should not block for readpartial" do
    server = TCPServer.new(0)
    value = nil
    t = Thread.new {
      sock = accept(server)
      value = sock.readpartial(2)
    }
    s = connect(server)
    wait_for_sleep_and_terminate(t) do
      t.alive?.should == true
      s.write("foo\r\n")
    end
    value.should == "fo"
  end

  it "should not block for sysread" do
    server = TCPServer.new(0)
    value = nil
    t = Thread.new {
      sock = accept(server)
      value = sock.sysread(2)
    }
    s = connect(server)
    wait_for_sleep_and_terminate(t) do
      t.alive?.should == true
      s.write("foo\r\n")
    end
    value.should == "fo"
  end

  it "should not block for sysread in ST condition" do
    server = TCPServer.new(0)
    client = TCPSocket.new('localhost', server.addr[1])
    sock = accept(server)
    begin
      sock.read_nonblock(5)
    rescue SystemCallError => e
      [Errno::EAGAIN, Errno::EWOULDBLOCK].include?(e.class).should == true
    end
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
        value = 1
        sock.write(BIG_CHUNK) # this blocks; [ruby-dev:26405]
      rescue RuntimeError
        value = 2
      end
    }
    s = connect(server)
    Thread.pass until value == 1
    wait_for_sleep_and_terminate(t) do
      t.raise # help thread termination
    end
    value.should == 2
  end

  it "should not block for write_nonblock" do
    server = TCPServer.new(0)
    value = nil
    t = Thread.new {
      sock = accept(server)
      value = sock.write_nonblock(BIG_CHUNK)
    }
    s = connect(server)
    wait_for_sleep_and_terminate(t)
    t.alive?.should == false
    value.should > 0
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
    yield if block_given?
    wait_for_terminate(server_thread)
  end

  def wait_for_sleep(t)
    timeout(2) do
      sleep 0.1 while t.status == 'run'
    end
  end

  def wait_for_terminate(t)
    timeout(2) do
      sleep 0.1 while t.alive?
    end
  end
end
