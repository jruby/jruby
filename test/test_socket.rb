require 'test/unit'
require 'socket'
require 'thread'
require 'test/test_helper'

class SocketTest < Test::Unit::TestCase
  include TestHelper

  def test_tcp_socket_allows_nil_for_hostname
    assert_nothing_raised do
      server = TCPServer.new(nil, 7789)
      t = Thread.new do
        s = server.accept
        s.close
      end
      client = TCPSocket.new(nil, 7789)
      client.write ""
      t.join
    end
  end

  #JRUBY-3827
  def test_nil_hostname_and_passive_returns_inaddr_any
    assert_nothing_raised do
      addrs = Socket::getaddrinfo(nil, 7789, Socket::AF_UNSPEC, Socket::SOCK_STREAM, 0, Socket::AI_PASSIVE)
      assert_equal(1, addrs.size)
      assert_equal("0.0.0.0", addrs[0][2])
      assert_equal("0.0.0.0", addrs[0][3])
    end
  end

  def test_nil_hostname_and_no_flags_returns_localhost
    assert_nothing_raised do
      addrs = Socket::getaddrinfo(nil, 7789, Socket::AF_UNSPEC, Socket::SOCK_STREAM, 0)
      assert_equal(1, addrs.size)
      
      # FIXME, behaves differently on Windows, both JRuby and MRI.
      # JRuby returns "127.0.0.1", "127.0.0.1"
      # MRI returns  "<actual_hostname>", "127.0.0.1"
      unless WINDOWS
        assert_equal("localhost", addrs[0][2])
        assert_equal("127.0.0.1", addrs[0][3])
      end
    end
  end

  def test_basic_socket_reverse_lookup
    assert_nothing_raised do
      reverse = BasicSocket.do_not_reverse_lookup
      BasicSocket.do_not_reverse_lookup = !reverse
      assert_equal(reverse, !BasicSocket.do_not_reverse_lookup)
      BasicSocket.do_not_reverse_lookup = reverse
    end
  end
  
  #JRUBY-2147
  def test_tcp_close_read
    socket = TCPServer.new(nil, 9999)
    socket.close_read
    assert(!socket.closed?)
    socket.close
  end
  
  #JRUBY-2146
  def test_tcp_close_write
    socket = TCPServer.new(nil, 8888)
    socket.close_write
    assert(!socket.closed?)
    socket.close
  end
  
  def test_tcp_close_read_then_write_should_close_socket
    socket = TCPServer.new(nil, 7777)
    socket.close_write
    assert(!socket.closed?)
    socket.close_read
    assert(socket.closed?)
  end

  # JRUBY-2874
  def test_raises_socket_error_on_out_of_range_port
    [-2**16, -2**8, -2, -1, 2**16, 2**16 + 1, 2**17, 2**30 -1].each do |port|
      assert_raises(SocketError) do
        TCPSocket.new('localhost', port)
      end
    end
  end
end

class UNIXSocketTests < Test::Unit::TestCase
  # this is intentional, otherwise test run fails on windows
  def test_dummy; end

  if defined?(UNIXSocket)
    def test_unix_socket_path
      path = "/tmp/sample"

      File.unlink(path) if File.exist?(path)
      
      server = UNIXServer.open(path)
      assert_equal path, server.path
      
      cli = UNIXSocket.open(path)

      assert_equal "", cli.path
      
      cli.close
      server.close
      File.unlink(path) if File.exist?(path)
    end

    def test_unix_socket_addr
      path = "/tmp/sample"

      File.unlink(path) if File.exist?(path)
      
      server = UNIXServer.open(path)
      assert_equal ["AF_UNIX", path], server.addr
      
      cli = UNIXSocket.open(path)

      assert_equal ["AF_UNIX", ""], cli.addr
      
      cli.close
      server.close
      File.unlink(path) if File.exist?(path)
    end

    def test_unix_socket_peeraddr_raises_enotconn
      path = "/tmp/sample"
      File.unlink(path) if File.exist?(path)

      server = UNIXServer.open(path)
      assert_raises(Errno::ENOTCONN) do
        server.peeraddr
      end
      File.unlink(path) if File.exist?(path)
    end

    def test_unix_socket_peeraddr
      path = "/tmp/sample"

      File.unlink(path) if File.exist?(path)
      
      server = UNIXServer.open(path)

      cli = UNIXSocket.open(path)

      ssrv = server.accept
      
      assert_equal ["AF_UNIX", ""], ssrv.peeraddr
      assert_equal ["AF_UNIX", path], cli.peeraddr

      ssrv.close
      cli.close
      server.close
      File.unlink(path) if File.exist?(path)
    end

    def test_unix_socket_raises_exception_on_too_long_path
      assert_raises(ArgumentError) do 
        # on some platforms, 103 is invalid length (MacOS)
        # on others, 108 (Linux), we'll take the biggest one
        UNIXSocket.new("a" * 108)
      end
    end
    
    def test_unix_socket_raises_exception_on_path_that_cant_exist 
      assert_raises(Errno::ENOENT) do 
        UNIXSocket.new("a")
      end
    end
    
    def test_can_create_socket_server
      path = "/tmp/sample"

      File.unlink(path) if File.exist?(path)
      
      sock = UNIXServer.open(path)
      assert File.exist?(path)
      sock.close

      File.unlink(path) if File.exist?(path)
    end

    def test_can_create_socket_server_and_accept_nonblocking
      path = "/tmp/sample"

      File.unlink(path) if File.exist?(path)
      
      sock = UNIXServer.open(path)
      assert File.exist?(path)

      assert_raises(Errno::EAGAIN) do 
        sock.accept_nonblock
      end

      cli = UNIXSocket.open(path)

      sock.accept_nonblock.close
      
      cli.close
      
      sock.close

      File.unlink(path) if File.exist?(path)
    end
    
    def test_can_create_socket_server_and_relisten
      path = "/tmp/sample"

      File.unlink(path) if File.exist?(path)
      
      sock = UNIXServer.open(path)
      
      assert File.exist?(path)

      sock.listen(1)

      assert File.exist?(path)

      sock.close

      File.unlink(path) if File.exist?(path)
    end

    def test_can_create_socket_server_and_client_connected_to_it
      path = "/tmp/sample"

      File.unlink(path) if File.exist?(path)
      
      sock = UNIXServer.open(path)
      assert File.exist?(path)
      
      cli = UNIXSocket.open(path)
      cli.close
      
      sock.close

      File.unlink(path) if File.exist?(path)
    end

    def test_can_create_socket_server_and_client_connected_to_it_and_send_from_client_to_server
      path = "/tmp/sample"
      File.unlink(path) if File.exist?(path)
      sock = UNIXServer.open(path)
      assert File.exist?(path)
      cli = UNIXSocket.open(path)
      servsock = sock.accept
      cli.send("hello",0)
      assert_equal "hello", servsock.recv(5)
      servsock.close
      cli.close
      sock.close
      File.unlink(path) if File.exist?(path)
    end

    def test_can_create_socket_server_and_client_connected_to_it_and_send_from_server_to_client
      path = "/tmp/sample"
      File.unlink(path) if File.exist?(path)
      sock = UNIXServer.open(path)
      assert File.exist?(path)
      cli = UNIXSocket.open(path)
      servsock = sock.accept
      servsock.send("hello",0)
      assert_equal "hello", cli.recv(5)
      servsock.close
      cli.close
      sock.close
      File.unlink(path) if File.exist?(path)
    end


    def test_can_create_socket_server_and_client_connected_to_it_and_send_from_client_to_server_using_recvfrom
      path = "/tmp/sample"
      File.unlink(path) if File.exist?(path)
      sock = UNIXServer.open(path)
      assert File.exist?(path)
      cli = UNIXSocket.open(path)
      servsock = sock.accept
      cli.send("hello",0)
      assert_equal ["hello", ["AF_UNIX", ""]], servsock.recvfrom(5)
      servsock.close
      cli.close
      sock.close
      File.unlink(path) if File.exist?(path)
    end

    def test_can_create_socket_server_and_client_connected_to_it_and_send_from_server_to_client_using_recvfrom
      path = "/tmp/sample"
      File.unlink(path) if File.exist?(path)
      sock = UNIXServer.open(path)
      assert File.exist?(path)
      cli = UNIXSocket.open(path)
      servsock = sock.accept
      servsock.send("hello",0)
      data = cli.recvfrom(5)
      assert_equal "hello", data[0]
      assert_equal "AF_UNIX", data[1][0]
      servsock.close
      cli.close
      sock.close
      File.unlink(path) if File.exist?(path)
    end

    def test_can_create_socketpair_and_send_from_one_to_the_other
      sock1, sock2 = UNIXSocket.socketpair
      
      sock1.send("hello", 0)
      assert_equal "hello", sock2.recv(5)
      
      sock1.close
      sock2.close
    end

    def test_can_create_socketpair_and_can_send_from_the_other
      sock1, sock2 = UNIXSocket.socketpair
      
      sock2.send("hello", 0)
      assert_equal "hello", sock1.recv(5)
      
      sock2.close
      sock1.close
    end

    def test_can_create_socketpair_and_can_send_from_the_other_with_recvfrom
      sock1, sock2 = UNIXSocket.socketpair
      
      sock2.send("hello", 0)
      assert_equal ["hello", ["AF_UNIX", ""]], sock1.recvfrom(5)
      
      sock2.close
      sock1.close
    end
    
    def test_can_read_and_get_minus_one
      sock1, sock2 = UNIXSocket.socketpair
      
      sock2.send("hello", 0)
      assert_equal "hell", sock1.recv(4)
      assert_equal "", sock1.recv(0)
      assert_equal "o", sock1.recv(1)

      sock2.close
      sock1.close
      
      assert_raises(IOError) do 
        sock1.recv(1)
      end
    end

  end
end

class ServerTest < Test::Unit::TestCase
  def test_server_close_interrupts_pending_accepts
    # unfortunately this test is going to not be 100% reliable
    # since it involves thread interaction and it's impossible to
    # do things like wait until the other thread blocks
    port = 41258
    server = TCPServer.new('localhost', port)
    queue = Queue.new
    thread = Thread.new do
      server.accept
    end
    # wait until the thread is sleeping (ready to accept)
    Thread.pass while thread.alive? && thread.status != "sleep"
    # close the server
    server.close
    # propagate the thread's termination error, checking it for IOError
    assert_raise(IOError) {thread.value}
  end
  
    # JRUBY-2874
    def test_raises_socket_error_on_out_of_range_port
      [-2**16, -2**8, -2, -1, 2**16, 2**16 + 1, 2**17, 2**30 -1].each do |port|
        assert_raises(SocketError) do
          TCPServer.new('localhost', port)
        end
      end
    end
end
