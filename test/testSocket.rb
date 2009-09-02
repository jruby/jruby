require 'test/minirunit'
require 'socket'

server_read = nil
client_read = nil

serv = TCPServer.new('localhost',2202)
server_thread = Thread.new do
  sock = serv.accept

  server_read = sock.read(5)
  sock.write "world!"
  sock.close
  serv.close
end

# This test is seriously broken, prone to race conditions and sometimes fail. This is why the rescue nil is there.

begin
  socket = TCPSocket.new("localhost",2202) 
  socket.write "Hello"
  client_read = socket.read(6)
  socket.close
  server_thread.join

  test_equal("Hello", server_read)
  test_equal("world!", client_read)
rescue
end

serv = TCPServer.new('localhost',2203)
test_no_exception { serv.listen(1024) } # fix for listen blowing up because it tried to rebind; it's a noop now
serv.close

# test block behavior for TCPServer::open
test_no_exception {
  TCPServer.open('localhost', 2204) {|sock| test_equal(TCPServer, sock.class)}
  TCPServer.open('localhost', 2204) {}
}

test_exception { serv.close }

### UDP ###

test_ok(UDPSocket::open)
# JRUBY-3849
test_ok(UDPSocket::new(Socket::AF_INET))

port = 4321

received = []

server = UDPSocket.open
server.bind(nil, port)
server_thread = Thread.start do
  2.times { received << server.recvfrom(64) } 
  server.close
end

# Ad-hoc client
UDPSocket.open.send("ad hoc", 0, 'localhost', port)

# Connection based client
sock = UDPSocket.open
sock.connect('localhost', port)
sock.send("connection-based", 0)
server_thread.join

# We can't check port, out of our control
# FIXME host may not be localhost if assigned a different name on some systems; we need a better way to test this

test_equal("ad hoc", received[0][0])
test_equal("AF_INET", received[0][1][0])
#test_ok(/^localhost/ =~ received[0][1][2])
test_equal("127.0.0.1", received[0][1][3])

test_equal("connection-based", received[1][0])
test_equal("AF_INET", received[1][1][0])
#test_ok(/^localhost/ =~ received[1][1][2])
test_equal("127.0.0.1", received[1][1][3])

test_equal(nil, sock.close)

# test_exception(SocketError) { UDPSocket.open.send("BANG!", -1, 'invalid.', port) }

# Test UDPSocket using recv instead of recvfrom

# server thread echoes received data
server_socket = UDPSocket.open
server_socket.bind(nil, port)
server_thread = Thread.start do
  data, remote_info = server_socket.recvfrom(64)
  server_socket.send(data, 0, remote_info[3], remote_info[1])
  server_socket.close
end

client_socket = UDPSocket.open
client_socket.send("udp recv", 0, "localhost", port) 
server_thread.join
received = client_socket.recv(64)

test_equal("udp recv", received)
test_equal(nil, client_socket.close)

# JRUBY-2005: test a large write that causes the buffer to flush
# make sure the result is not corrupted
serv = TCPServer.new('localhost',2202)
t = Thread.new {
  sock = serv.accept

  result = ''
  until result.length == 20010
    result << sock.sysread(20010 - result.length)
  end
  sock.close
  serv.close

  result
}

# 20k string is larger than our default 16k buffer
# FIXME: of course if our buffer size changes
str = "0123456789" * 2000
client = TCPSocket.new('localhost', 2202)
# first ten chars should get buffered
client.write('abcdefghij')
# large write should force buffer to flush before write
client.write(str)
client.close

t.join

test_equal('abcdefghij' + str, t.value)

#JRUBY-2666
empty_host_addr_info = Socket::getaddrinfo("", "http", Socket::AF_INET, Socket::SOCK_STREAM,
  Socket::IPPROTO_TCP, Socket::AI_PASSIVE)
test_equal([["AF_INET", 80, "0.0.0.0", "0.0.0.0", Socket::AF_INET, Socket::SOCK_STREAM, Socket::IPPROTO_TCP]], empty_host_addr_info)

# test that raising inside an accepting thread doesn't nuke the socket
# ** Currently FAILING on Windows and Solaris -- the
# ** 't.raise' is happening before the socket gets fully into #accept
# accepted_latch = Java::java.util.concurrent.CountDownLatch.new(1)
# closed_latch   = Java::java.util.concurrent.CountDownLatch.new(1)
# tcp = TCPServer.new(nil, 5000)
# exception = nil
# t = Thread.new {
#   begin
#     accepted_latch.count_down
#     tcp.accept
#   rescue Exception => e
#     exception = e
#     # this would normally blow up if the socket was demolished
#     tcp.close
#   ensure
#     closed_latch.count_down
#   end
# }
# accepted_latch.await
# t.raise
# closed_latch.await
# test_ok RuntimeError === exception
