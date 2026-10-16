require 'test/unit'
require 'test/jruby/test_helper'
require 'socket'
require 'thread'
require 'ipaddr'
require 'tmpdir'

class SocketTest < Test::Unit::TestCase
  include TestHelper

  if !WINDOWS # #5656 is tracking issue for this.
    def test_multicast_send_and_receive
      multicast_addr, port = "225.4.5.6", 6789
      multicast_msg = "Hello from automated JRuby test suite"
  
      assert_nothing_raised do
        socket = UDPSocket.new
        ip =  IPAddr.new(multicast_addr).hton + IPAddr.new("0.0.0.0").hton
        socket.setsockopt(Socket::IPPROTO_IP, Socket::IP_ADD_MEMBERSHIP, ip)
        socket.bind(Socket::INADDR_ANY, port)
        socket.send(multicast_msg, 0, multicast_addr, port)
        msg, info = socket.recvfrom(1024)
        assert_equal(multicast_msg, msg)
        assert_equal(multicast_msg.size, msg.size)
        assert_equal(port, info[1])
        socket.close
      end
    end
  end

  def test_tcp_socket_allows_nil_for_hostname
    assert_nothing_raised do
      server = TCPServer.new(nil, 7789)
      begin
        t = Thread.new do
          s = server.accept
          s.close
        end
        client = TCPSocket.new(nil, 7789)
        client.write ""
        t.join
      ensure
        server.close if server && !server.closed?
      end
    end
  end

  if RbConfig::CONFIG['target_os'] == 'linux'
    def test_tcp_info
      assert_nothing_raised do
        server = TCPServer.new(nil, 7789)
        begin
          t = Thread.new do
            s = server.accept
            s.close
          end
          client = TCPSocket.new(nil, 7789)
          t.join
          tcp_info = client.getsockopt(Socket::IPPROTO_TCP, Socket::TCP_INFO)
          state = tcp_info.unpack("C")[0]
          assert_equal(8, state) # CLOSE_WAIT
        ensure
          server.close if server && !server.closed?
        end
      end
    end
  end

  if RbConfig::CONFIG['target_os'] == 'linux'
    def test_not_available_constant
      assert !Socket.const_defined?(:TCP_KEEPALIVE)
    end
  end

  #JRUBY-3827
  def test_nil_hostname_and_passive_returns_inaddr_any
    assert_nothing_raised do
      addrs = Socket::getaddrinfo(nil, 7789, Socket::AF_UNSPEC, Socket::SOCK_STREAM, 0, Socket::AI_PASSIVE)
      assert_not_equal(0, addrs.size)
      assert_equal("0.0.0.0", addrs[0][2])
      assert_equal("0.0.0.0", addrs[0][3])
    end
  end

  def test_nil_hostname_and_no_flags_returns_localhost
    assert_nothing_raised do
      addrs = Socket::getaddrinfo(nil, 7789, Socket::AF_UNSPEC, Socket::SOCK_STREAM, 0)
      assert_not_equal(0, addrs.size)

      # FIXME, behaves differently on Windows, both JRuby and MRI.
      # JRuby returns "127.0.0.1", "127.0.0.1"
      # MRI returns  "<actual_hostname>", "127.0.0.1"
      unless WINDOWS
        #assert_equal("localhost", addrs[0][2])
        assert_equal("127.0.0.1", addrs[0][3])
      end
    end
  end

  def test_getifaddrs
    begin
      list = Socket.getifaddrs
    rescue NotImplementedError
      return
    end
    list.each {|ifaddr|
      assert_instance_of(Socket::Ifaddr, ifaddr)
    }
  end

  def test_getifaddrs_packet_interfaces
    begin
      list = Socket.getifaddrs
    rescue NotImplementedError
      return
    end
    ifnames = list.collect(&:name).uniq
    ifnames.each do |ifname|
      packet_interfaces = list.select { |ifaddr| ifaddr.name == ifname && ifaddr.addr.afamily == Socket::AF_UNSPEC } # TODO: (gf) Socket::AF_PACKET when available
      assert_equal(1, packet_interfaces.count) # one for each interface
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
    port = -2**16
    assert_raises(SocketError) { TCPSocket.new('localhost', port) }
    # SocketError(<getaddrinfo: Servname not supported for ai_socktype>)

    port = -2**8
    assert_raises(SocketError) { TCPSocket.new('localhost', port) }
    # SocketError(<getaddrinfo: Servname not supported for ai_socktype>)

    port = -2
    assert_raises(SocketError) { TCPSocket.new('localhost', port) }
    # SocketError(<getaddrinfo: Servname not supported for ai_socktype>)

    port = -1
    assert_raises(SocketError) { TCPSocket.new('localhost', port) }
    # SocketError(<getaddrinfo: Servname not supported for ai_socktype>)

    error = defined?(JRUBY_VERSION) ? SocketError : Errno::ECONNREFUSED
    [ 2**16, 2**16 + 1, 2**17, 2**30 - 1 ].each do |p|
      assert_raises(error) { TCPSocket.new('localhost', p) }
    end
  end

  if RbConfig::CONFIG['target_os'] == 'linux'
    def test_tcp_socket_get_keep_idle
      socket = Socket.new(Socket::AF_INET, Socket::SOCK_STREAM, 0)
      assert_instance_of(Integer, socket.getsockopt(Socket::SOL_TCP, Socket::TCP_KEEPIDLE).int)
    ensure
      socket.close
    end

    def test_tcp_socket_get_keep_intvl
      socket = Socket.new(Socket::AF_INET, Socket::SOCK_STREAM, 0)
      assert_instance_of(Integer, socket.getsockopt(Socket::SOL_TCP, Socket::TCP_KEEPINTVL).int)
    ensure
      socket.close
    end

    def test_tcp_socket_get_keep_cnt
      socket = Socket.new(Socket::AF_INET, Socket::SOCK_STREAM, 0)
      assert_instance_of(Integer, socket.getsockopt(Socket::SOL_TCP, Socket::TCP_KEEPCNT).int)
    ensure
      socket.close
    end

    def test_tcp_socket_set_keep_idle
      socket = Socket.new(Socket::AF_INET, Socket::SOCK_STREAM, 0)
      assert_equal 0, socket.setsockopt(Socket::SOL_TCP, Socket::TCP_KEEPIDLE, 1)
    ensure
      socket.close
    end

    def test_tcp_socket_set_keep_intvl
      socket = Socket.new(Socket::AF_INET, Socket::SOCK_STREAM, 0)
      assert_equal 0, socket.setsockopt(Socket::SOL_TCP, Socket::TCP_KEEPINTVL, 1)
    ensure
      socket.close
    end

    def test_tcp_socket_set_keep_cnt
      socket = Socket.new(Socket::AF_INET, Socket::SOCK_STREAM, 0)
      assert_equal 0, socket.setsockopt(Socket::SOL_TCP, Socket::TCP_KEEPCNT, 1)
    ensure
      socket.close
    end
  end

  # JRUBY-4299
  def test_tcp_socket_reuse_addr
    socket = Socket.new(Socket::AF_INET, Socket::SOCK_STREAM, 0)

    socket.setsockopt(Socket::SOL_SOCKET, Socket::SO_REUSEADDR, true)
    assert_not_equal 0, socket.getsockopt(Socket::SOL_SOCKET, Socket::SO_REUSEADDR).unpack('i')[0]
  ensure
    socket.close
  end

  # JRUBY-4299
  def test_udp_socket_reuse_addr
    socket = Socket.new(Socket::AF_INET, Socket::SOCK_DGRAM, 0)

    socket.setsockopt(Socket::SOL_SOCKET, Socket::SO_REUSEADDR, true)
    assert_not_equal 0, socket.getsockopt(Socket::SOL_SOCKET, Socket::SO_REUSEADDR).unpack('i')[0]
  ensure
    socket.close
  end

  # JRUBY-4868
  def test_getservbyname
    assert_equal(21, Socket.getservbyname('ftp'))
    assert_equal(21, Socket.getservbyname('21'))
    assert_equal(21, Socket.getservbyname('       21'))
  end

  def test_ipv4_socket
    socket = Socket.new(:INET, :STREAM)
    server_socket = ServerSocket.new(:INET, :STREAM)

    addr = Addrinfo.tcp('0.0.0.0', 3030)
    addr1 = Addrinfo.tcp('0.0.0.0', 3031)

    assert_not_equal(socket.bind(addr), nil)

    assert_not_equal(server_socket.bind(addr1), nil)
    assert_not_equal(server_socket.listen(128), nil)
  end

  def test_udp_socket_bind
    begin
      UDPSocket.new.bind nil, 42
    rescue Errno::EACCES => e
      # Permission denied - bind(2) for nil port 42
      assert_equal 'Permission denied - bind(2) for nil port 42', e.message
    else; fail 'not raised'
    end unless WINDOWS # MRI (on Windows) returns 0

    #begin
    #  UDPSocket.new.bind nil, nil
    #rescue SocketError
    #  # getaddrinfo: Name or service not known
    #else; fail 'not raised'
    #end

    begin
      socket = UDPSocket.new
      socket.bind 0, 42
    rescue Errno::EACCES
      # Permission denied - bind(2) for 0 port 42
    else; fail 'not raised'
    end unless WINDOWS # MRI (on Windows) returns 0 (while triggering a Security Alert)

    begin
      UDPSocket.new.bind "127.0.0.1", 191
    rescue Errno::EACCES
      # Permission denied - bind(2) for "127.0.0.1" port 191
    else; fail 'not raised'
    end unless WINDOWS # MRI (on Windows) returns 0
  end

  def test_tcp_socket_errors
    begin
      TCPSocket.new('0.0.0.0', 42)
    rescue Errno::ECONNREFUSED => e
      # Connection refused - connect(2) for "0.0.0.0" port 42
      assert_equal 'Connection refused - connect(2) for "0.0.0.0" port 42', e.message
    else; fail 'not raised'
    end

    server = TCPServer.new('127.0.0.1', 10022)
    pend 'TODO: IOError: closed stream (on Windows)' if WINDOWS
    Thread.new { server.accept }
    socket = TCPSocket.new('127.0.0.1', 10022)
    begin
      socket.read_nonblock 100
    rescue IO::EAGAINWaitReadable
      # Resource temporarily unavailable - read would block
    else; fail 'not raised'
    ensure
      server.close rescue nil
      socket.close rescue nil
    end
  end

  def test_connect_nonblock_no_exception
    serv = ServerSocket.new(:INET, :STREAM)
    serv.bind(Socket.sockaddr_in(0, "127.0.0.1"), 5)
    c = Socket.new(:INET, :STREAM)
    servaddr = serv.getsockname
    rv = c.connect_nonblock(servaddr, exception: false)
    case rv
    when 0
      # some OSes return immediately on non-blocking local connect()
    else
      assert_equal :wait_writable, rv
    end
    assert_equal([ [], [c], [] ], IO.select(nil, [c], nil, 60))
    assert_equal(0, c.connect_nonblock(servaddr, exception: false),
                 'there should be no EISCONN error')
  ensure
    serv.close if serv
    c.close if c
  end

  # IO.select in another thread leaves the channel nonblocking, where ServerSocket#accept used to raise EAGAIN
  def test_server_socket_accept_waits_while_another_thread_selects_on_it
    server = ServerSocket.new(:INET, :STREAM)
    server.bind(Socket.sockaddr_in(0, "127.0.0.1"), 5)
    selector = Thread.new { IO.select([server]) }
    Thread.pass until selector.status == "sleep"
    acceptor = Thread.new { server.accept }
    Thread.pass until acceptor.status == "sleep" || !acceptor.alive?

    client = TCPSocket.new("127.0.0.1", server.local_address.ip_port)

    assert acceptor.join(10), "accept did not return"
    socket, addrinfo = acceptor.value
    assert_instance_of Socket, socket
    assert_equal client.local_address.ip_port, addrinfo.ip_port
    assert selector.join(10), "IO.select did not return"
  ensure
    socket&.close
    client&.close
    server&.close
  end

end


class UNIXSocketTests < Test::Unit::TestCase
  include TestHelper

  # this is intentional, otherwise test run fails on windows
  def test_dummy; end

  if defined?(UNIXSocket) && !WINDOWS
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
       path = "a"
       File.unlink(path) if File.exist?(path)
       assert_raises(Errno::ENOENT) do
         UNIXSocket.new(path)
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
       pending_hangs_on_macos_aarch64
       path = "/tmp/sample"

       File.unlink(path) if File.exist?(path)

       sock = UNIXServer.open(path)
       assert File.exist?(path)

       begin
         sock.accept_nonblock
         assert false, "failed to raise EAGAIN"
       rescue Errno::EAGAIN => e
         assert IO::WaitReadable === e
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

     # JRUBY-5708
     def test_can_create_socket_server_and_blocking_select_blocks_on_it
       require 'timeout'

       path = "/tmp/sample"

       File.unlink(path) if File.exist?(path)

       sock = UNIXServer.open(path)

       assert File.exist?(path)

       assert_raises(Timeout::Error) do
         Timeout::timeout(0.1) do
           IO.select [sock], nil, nil, 1
         end
       end

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
        assert_equal nil, sock1.recv(0)
        assert_equal "o", sock1.recv(1)

        sock2.close
        sock1.close

        assert_raises(IOError) do
          sock1.recv(1)
        end
      end

      def test_recv_nonblock
        pending_hangs_on_macos_aarch64
        s1, s2 = UNIXSocket.pair(Socket::SOCK_DGRAM)
        begin
          s1.recv_nonblock(1)
          assert false
        rescue => e
          assert(IO::EAGAINWaitReadable === e)
          assert(IO::WaitReadable === e)
        end
        # TODO '' does not get through as expected :
        #s2.send('', 0)
        #assert_equal '', s1.recv_nonblock(10, nil)
        begin
          s1.recv_nonblock(10, nil)
          assert false
        rescue IO::EAGAINWaitReadable
        end
        s2.send('a', 0)
        s1.recv_nonblock(5, nil, str = +'')
        assert_equal 'a', str
        assert_raise(IO::EAGAINWaitReadable) { s1.recv_nonblock(5, nil, str) }
        assert_equal :wait_readable, s1.recv_nonblock(5, exception: false)
      end

      # accept_nonblock in another thread can take the connection this thread's select reported while the channel
      # is nonblocking, which made UNIXServer#accept return a socket without a connection
      def test_unix_server_accept_waits_again_when_another_thread_takes_the_connection
        Dir.mktmpdir do |dir|
          path = File.join(dir, "accept.sock")
          server = UNIXServer.new(path)
          accepted = Queue.new
          stolen = Queue.new
          stop = false

          acceptor = Thread.new do
            accepted << server.accept until stop
          end
          # woken by the same connections as the acceptor, like an event loop
          thief = Thread.new do
            until stop
              IO.select([server], nil, nil, 0.1)
              socket = server.accept_nonblock(exception: false)
              stolen << socket unless socket == :wait_readable
            end
          end
          Thread.pass until acceptor.status == "sleep"

          # spaced out, so both threads are waiting when each connection arrives; keep connecting until the thief
          # took a few, which is when the race happens
          clients = []
          until stolen.size >= 3 || clients.size >= 300
            clients << UNIXSocket.new(path)
            sleep 0.002
          end
          deadline = Process.clock_gettime(Process::CLOCK_MONOTONIC) + 10
          Thread.pass until accepted.size + stolen.size >= clients.size || Process.clock_gettime(Process::CLOCK_MONOTONIC) > deadline

          sockets = Array.new(accepted.size) { accepted.pop }
          unconnected = sockets.count do |socket|
            socket.remote_address
            false
          rescue
            true
          end
          assert_equal 0, unconnected
          assert_equal clients.size, sockets.size + stolen.size
          omit "the thief took no connection, so the race was not exercised" if stolen.empty?
        ensure
          # the acceptor needs one more connection to see stop; closing the server under it would race the close
          stop = true
          stop_deadline = Process.clock_gettime(Process::CLOCK_MONOTONIC) + 10
          while [acceptor, thief].any? { |thread| thread&.alive? } && Process.clock_gettime(Process::CLOCK_MONOTONIC) < stop_deadline
            (clients ||= []) << UNIXSocket.new(path)
            sleep 0.01
          end
          [*clients, *sockets].each { |io| io.close rescue nil }
          accepted.size.times { accepted.pop.close rescue nil } if accepted
          stolen.size.times { stolen.pop.close rescue nil } if stolen
          server&.close
        end
      end

      # sysaccept hands the descriptor to the caller, so collecting the UNIXSocket it was accepted into must not close it
      def test_unix_server_sysaccept_descriptor_outlives_the_accepted_socket_object
        Dir.mktmpdir do |dir|
          path = File.join(dir, "sysaccept.sock")
          server = UNIXServer.new(path)
          clients = 10.times.map { UNIXSocket.new(path) }
          # created in other threads, so no frame of this one keeps those objects reachable
          fds = Thread.new { clients.map { server.sysaccept } }.value
          dropped_fds = Thread.new { UNIXSocket.pair.map(&:fileno) }.value
          fileno_util = JRuby.runtime.fileno_util
          registered = -> fd { fileno_util.get_wrapper_from_fileno(fd) }

          # collect until the finalizers closed the dropped pair, which shows the collection reached unreferenced sockets
          deadline = Time.now + 5
          (java.lang.System.gc; sleep 0.01) while dropped_fds.any?(&registered) && Time.now < deadline
          assert dropped_fds.none?(&registered), "unreferenced sockets were not finalized, so this test proves nothing"

          sockets = fds.map { |fd| Socket.for_fd(fd) }
          clients.each_with_index { |client, i| client.write(i.to_s) }
          assert_equal clients.size.times.map(&:to_s), sockets.map { |socket| socket.read(1) }
        ensure
          sockets&.each(&:close)
          clients&.each(&:close)
          server&.close
        end
      end

    end
  private

  def pending_hangs_on_macos_aarch64
    pend "hangs on MacOS/ARM64" if RbConfig::CONFIG['host_os'] =~ /darwin/
  end
end

class ServerTest < Test::Unit::TestCase
  include TestHelper

  def test_server_close_interrupts_pending_accepts
    # unfortunately this test is going to not be 100% reliable
    # since it involves thread interaction and it's impossible to
    # do things like wait until the other thread blocks
    port = 41258
    server = TCPServer.new('localhost', port)
    pend 'TODO: IOError: stream closed in another thread (on Windows)' if WINDOWS
    thread = Thread.new do
      server.accept
    end
    # wait until the thread is sleeping (ready to accept)
    Thread.pass while thread.alive? && thread.status != "sleep"
    # close the server
    server.close
    # propagate the thread's termination error, checking it for IOError
    # NOTE: 1.8 raises IOError, 1.9 EBADF, so this isn't consistent. I'm
    # changing it to Exception so we can at least test the interrupt.
    assert_raise(IOError) {thread.value}
  end

  # JRUBY-2874
  def test_raises_socket_error_on_out_of_range_port
    [-2**16, -2**8, -2, -1, 2**16, 2**16 + 1, 2**17, 2**30 - 1].each do |p|
      assert_raises(SocketError) do
        TCPServer.new('localhost', p)
      end
    end
  end

  # JRUBY-4299
  def test_server_reuse_addr
    socket = TCPServer.new("127.0.0.1", 7777)

    socket.setsockopt(Socket::SOL_SOCKET, Socket::SO_REUSEADDR, true)
    assert_not_equal 0, socket.getsockopt(Socket::SOL_SOCKET, Socket::SO_REUSEADDR).unpack('i')[0]
  ensure
    socket.close
  end

  # JRUBY-5111
  def test_server_methods_with_closed_socket
    socket = TCPServer.new("127.0.0.1", 7777)
    socket.close
    assert_raises(IOError) { socket.addr }
    assert_raises(IOError) { socket.getsockname }
  end

  # JRUBY-5876
  def test_syswrite_raises_epipe
    t = Thread.new do
      server = TCPServer.new("127.0.0.1", 1234)
      while sock = server.accept
        sock.close
      end
    end

    Thread.pass while t.alive? and t.status != 'sleep'

    sock = TCPSocket.new("127.0.0.1", 1234)
    sock.setsockopt Socket::IPPROTO_TCP, Socket::TCP_NODELAY, 1

    delay = 0.1
    tries = 0
    loop do
      sock.syswrite("2")
    end
  rescue Errno::EPIPE, Errno::ECONNRESET
    # ok
  rescue => ex
    # FIXME: make Windows behave the same?
    raise ex if !WINDOWS
  ensure
    t.kill rescue nil
    server.close rescue nil
    sock.close rescue nil
  end

  # jruby/jruby#1637
  def test_read_zero_never_blocks
    require 'timeout'
    assert_nothing_raised do
      server = TCPServer.new(nil, 12345)
      t = Thread.new do
        s = server.accept
      end
      client = TCPSocket.new(nil, 12345)
      Timeout.timeout(1) do
        assert_equal "", client.read(0)
      end
      t.join
    end
  ensure
    server.close rescue nil
    client.close rescue nil
  end if RUBY_VERSION >= '1.9'
end

# accept and sysaccept wait through Fiber::Scheduler#io_wait instead of blocking the fiber's thread
class AcceptFiberSchedulerTest < Test::Unit::TestCase
  include TestHelper

  TIMEOUT = 10

  # Records each io_wait and fiber_interrupt and parks the fiber until the test resumes it with what the hook
  # returns, like FiberSpecs::LoggingScheduler. io_wait also records whether the nonblocking accept attempt put the
  # channel back in blocking mode.
  class WaitScheduler
    attr_reader :io_waits, :interrupts

    def initialize
      @io_waits = []
      @interrupts = []
    end

    def io_wait(io, events, timeout)
      @io_waits << [io, events, timeout, io.to_channel.blocking?]
      Fiber.yield
    end

    def fiber_interrupt(fiber, exception)
      @interrupts << [fiber, exception]
      Fiber.yield
    end

    # required by Fiber.set_scheduler, unused by these tests
    def block(blocker, timeout = nil)
      Fiber.yield
    end

    def unblock(blocker, fiber)
    end

    def kernel_sleep(duration = nil)
      Fiber.yield
    end
  end

  def test_server_socket_accept_takes_a_pending_connection_without_io_wait
    server = new_server_socket
    client = TCPSocket.new("127.0.0.1", server.local_address.ip_port)
    IO.select([server])
    accepted = nil

    scheduler = run_scheduled do
      accepted = Fiber.new(blocking: false) { server.accept }.resume
    end

    assert_instance_of Socket, accepted&.first
    assert_empty scheduler.io_waits
  ensure
    accepted&.first&.close
    client&.close
    server&.close
  end

  def test_server_socket_accept_waits_again_when_io_wait_returns_without_a_connection
    server = new_server_socket
    accepted = client = nil

    scheduler = run_scheduled do
      acceptor = Fiber.new(blocking: false) { server.accept }
      acceptor.resume
      acceptor.resume(IO::READABLE)
      client = TCPSocket.new("127.0.0.1", server.local_address.ip_port)
      IO.select([server])
      accepted = acceptor.resume(IO::READABLE)
    end

    assert_instance_of Socket, accepted&.first
    assert_equal 2, scheduler.io_waits.size
  ensure
    accepted&.first&.close
    client&.close
    server&.close
  end

  def test_server_socket_accept_raises_timeout_error_when_io_wait_returns_false
    server = new_server_socket

    run_scheduled do
      acceptor = Fiber.new(blocking: false) { server.accept }
      acceptor.resume
      error = assert_raises(IO::TimeoutError) { acceptor.resume(false) }
      assert_equal "Timed out waiting for IO to become readable!", error.message
    end
  ensure
    server&.close
  end

  def test_server_socket_accept_raises_ioerror_when_another_fiber_closes_the_server
    server = new_server_socket

    run_scheduled do |scheduler|
      acceptor = Fiber.new(blocking: false) { server.accept }
      acceptor.resume
      closer = Fiber.new(blocking: false) { server.close }
      closer.resume

      # deliver the interrupt as a scheduler would, then let close finish
      fiber, interrupt = scheduler.interrupts.last
      assert_same acceptor, fiber
      error = assert_raises(IOError) { acceptor.raise(interrupt) }
      assert_equal "stream closed in another thread", error.message
      closer.resume
    end

    assert server.closed?
  ensure
    server&.close
  end

  def test_server_socket_accept_does_not_call_the_scheduler_from_a_blocking_fiber
    server = new_server_socket
    accepted = connector = nil

    scheduler = run_scheduled do
      # a blocking ServerSocket#accept does not mark the thread as sleeping, so the client waits a little instead
      connector = Thread.new do
        sleep 0.1
        TCPSocket.new("127.0.0.1", server.local_address.ip_port)
      end
      accepted = server.accept
    end

    assert_instance_of Socket, accepted&.first
    assert_empty scheduler.io_waits
  ensure
    accepted&.first&.close
    connector&.value&.close
    server&.close
  end

  def test_server_socket_accept_lets_other_fibers_run_while_it_waits
    server = new_server_socket
    client_port = nil
    connect = -> { TCPSocket.new("127.0.0.1", server.local_address.ip_port).tap { |client| client_port = client.local_address.ip_port } }
    accepted = assert_accept_lets_other_fibers_run(server, connect) { server.accept }

    socket, addrinfo = accepted
    assert_instance_of Socket, socket
    assert_instance_of Addrinfo, addrinfo
    assert_equal client_port, addrinfo.ip_port
  ensure
    accepted&.first&.close
    server&.close
  end

  def test_tcp_server_accept_lets_other_fibers_run_while_it_waits
    server = TCPServer.new("127.0.0.1", 0)
    accepted = assert_accept_lets_other_fibers_run(server, -> { TCPSocket.new("127.0.0.1", server.local_address.ip_port) }) { server.accept }

    assert_instance_of TCPSocket, accepted
  ensure
    accepted&.close
    server&.close
  end

  def test_tcp_server_sysaccept_lets_other_fibers_run_while_it_waits
    server = TCPServer.new("127.0.0.1", 0)
    fd = assert_accept_lets_other_fibers_run(server, -> { TCPSocket.new("127.0.0.1", server.local_address.ip_port) }) { server.sysaccept }

    assert_kind_of Integer, fd
  ensure
    IO.for_fd(fd).close if fd.is_a?(Integer)
    server&.close
  end

  unless WINDOWS
    def test_unix_server_accept_lets_other_fibers_run_while_it_waits
      Dir.mktmpdir do |dir|
        path = File.join(dir, "accept.sock")
        server = UNIXServer.new(path)
        accepted = assert_accept_lets_other_fibers_run(server, -> { UNIXSocket.new(path) }) { server.accept }

        assert_instance_of UNIXSocket, accepted
      ensure
        accepted&.close
        server&.close
      end
    end

    def test_unix_server_sysaccept_lets_other_fibers_run_while_it_waits
      Dir.mktmpdir do |dir|
        path = File.join(dir, "sysaccept.sock")
        server = UNIXServer.new(path)
        fd = assert_accept_lets_other_fibers_run(server, -> { UNIXSocket.new(path) }) { server.sysaccept }

        assert_kind_of Integer, fd
      ensure
        Socket.for_fd(fd).close if fd.is_a?(Integer)
        server&.close
      end
    end
  end

  private

  def new_server_socket
    server = ServerSocket.new(:INET, :STREAM)
    server.bind(Socket.sockaddr_in(0, "127.0.0.1"), 5)
    server
  end

  # Runs the block in a new thread with a WaitScheduler, failing instead of hanging if accept blocks that thread or
  # waits on a lock a parked fiber holds. A stuck thread is not killed, since Thread#kill waits on a thread blocked on
  # a Java monitor; closing the server in the test's ensure releases it.
  def run_scheduled
    scheduler = WaitScheduler.new
    thread = Thread.new do
      Thread.current.report_on_exception = false
      Fiber.set_scheduler(scheduler)
      yield scheduler
    end

    assert thread.join(TIMEOUT), "the scheduler's thread did not finish within #{TIMEOUT}s"
    scheduler
  end

  # While one fiber waits to accept, a sibling fiber must be able to take the server's blocking lock (through
  # accept_nonblock), and the nonblocking accept attempt must have put the channel back in blocking mode.
  def assert_accept_lets_other_fibers_run(server, connect, &accept)
    accepted = client = nil

    scheduler = run_scheduled do
      acceptor = Fiber.new(blocking: false, &accept)
      acceptor.resume
      assert_equal :wait_readable, Fiber.new(blocking: false) { server.accept_nonblock(exception: false) }.resume
      client = connect.call
      IO.select([server])
      accepted = acceptor.resume(IO::READABLE)
    end

    assert_equal [[server, IO::READABLE, nil, true]], scheduler.io_waits
    accepted
  ensure
    client&.close
  end
end
