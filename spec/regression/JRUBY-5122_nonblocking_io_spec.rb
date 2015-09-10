require 'socket'
require 'timeout'
require 'fcntl'
require 'rbconfig'

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
    expect(value).to eq("foo\r\n")
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
    expect(value).to eq(false)
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
    expect(value).to eq(?f)
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
    expect(value).to eq(["foo\r\n", "bar\r\n"])
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
    expect(value).to eq("foo\r\nbar\r\nbaz")
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
      expect(t.alive?).to eq(true)
      s.write("foo\r\n")
    end
    expect(value).to eq("fo")
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
      expect(t.alive?).to eq(true)
      s.write("f")
      expect(t.alive?).to eq(true)
      s.write("oo\r\n")
    end
    expect(value).to eq("foo\r")
  end

  it "should read 4 bytes for read(4)" do
    100.times do
      server = TCPServer.new(0)
      value = nil
      t = Thread.new {
        sock = accept(server)
        value = sock.read(4)
      }
      s = connect(server)
      # 2 (or more?) times write is needed to reproduce
      # And writing "12" then "345" blocks forever.
      s.write("1")
      s.write("2345")
      t.join
      expect(value).to eq("1234")
    end
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
      expect(t.alive?).to eq(true)
      s.write("foo\r\n")
    end
    expect(value).to eq("fo")
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
      expect(t.alive?).to eq(true)
      s.write("foo\r\n")
    end
    expect(value).to eq("fo")
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
      expect(t.alive?).to eq(true)
      s.write("foobar")
      s.close
    end
    expect(value).to eq(114)
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
      expect(t.alive?).to eq(true)
      s.write("foo\r\nbar\r\nbaz")
      s.close
    end
    expect(value).to eq("baz")
  end

  # WRITE BLOCKAGE:
  #
  # We try to pick a suitably large value such that potentially-blocking
  # writes are more likely to reach buffer limits and actually block.
  #
  # On an Ubuntu 10.10(64) box:
  #   Packaged OpenJDK6 block with > 152606 (?)
  #   Oracle's build block with > 131072 (2**17)
  # On a Windows 7(64) box:
  #   Oracle's build does not block (use memory till OOMException)
  SOCKET_CHANNEL_MIGHT_BLOCK = "a" * (219463 * 4)

# This spec does not appear to test anything meaningful and occasionally
# failed due to several inherent races. I improved the race situation
# somewhat, but it's unclear whether this spec can ever fail since it
# appears to accept both blocking and nonblocking write.
#
# I believe the spec originally expected small writes not to block, which
# is reasonable, but at some point it mutated into a test that write
# *does* block under certain circumstances, making the original assertions
# meaningless.
#
# See jruby/jruby#2332

=begin
  it "should not block for write" do
    100.times do # for acceleration; it failed w/o wait_for_accepted call
      server = TCPServer.new(0)
      value = nil
      t = Thread.new {
        sock = accept(server)
        begin
          value = 1
          # this could block; [ruby-dev:26405]  But it doesn't block on Windows.
          sock.write(SOCKET_CHANNEL_MIGHT_BLOCK)
          value = 2
        rescue RuntimeError
          value = 3
        end
      }
      s = connect(server)

      # Whether write blocks or not, read will block until data is available
      IO.select([s], nil, nil, 2)

      # If write did not block, give thread some time to advance
      100.times { Thread.pass }

      # Now check where we are
      wait_for_sleep_and_terminate(t) do
        if value == 1

          # Write blocked [ruby-dev:26405], see WRITE BLOCKAGE above
          type = :blocked
          t.raise # help thread termination
          t.join

          if RbConfig::CONFIG['host_os'] !~ /mingw|mswin/
            value.should == 3
            t.status.should == false
          end

        else

          # Write did not block
          value.should == 2
          t.status.should == false
        end
      end
    end
  end
=end

  it "should not block for write_nonblock" do
    server = TCPServer.new(0)
    value = nil
    t = Thread.new {
      sock = accept(server)
      value = sock.write_nonblock(SOCKET_CHANNEL_MIGHT_BLOCK)
    }
    s = connect(server)
    wait_for_terminate(t)
    expect(value).to be > 0
  end

  def accept(server)
    sock = server.accept
    flag = File::NONBLOCK
    flag |= sock.fcntl(Fcntl::F_GETFL)
    sock.fcntl(Fcntl::F_SETFL, flag)
    Thread.current[:accepted] = true
    sock
  end

  def connect(server)
    TCPSocket.new('localhost', server.addr[1])
  end

  def wait_for_sleep_and_terminate(server_thread)
    wait_for_accepted(server_thread)
    wait_for_sleep(server_thread)
    yield if block_given?
    wait_for_terminate(server_thread)
  end

  def wait_for_accepted(server_thread)
    timeout(2) do
      Thread.pass while !server_thread[:accepted]
    end
  end

  def wait_for_sleep(t)
    timeout(2) do
      Thread.pass while t.status == 'run'
    end
  end

  def wait_for_terminate(t)
    timeout(2) do
      Thread.pass while t.alive?
    end
  end
end
