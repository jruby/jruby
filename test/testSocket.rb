require 'test/minirunit'
require 'socket'

server_read = nil
client_read = nil

server_thread = Thread.new do
  serv = TCPServer.new('localhost',2202)
  sock = serv.accept
  
  server_read = sock.read(5)
  sock.write "world!"
  sock.close
end

# This test is seriously broken, prone to race conditions and sometimes fail. This is why the rescue nil is there.
sleep 1

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

# test block behavior for TCPServer::open
test_no_exception {
  TCPServer.open('localhost', 2204) {|sock| test_equal(TCPServer, sock.class)}
  TCPServer.open('localhost', 2204) {}
}

### UDP ###

test_ok(UDPSocket::open)

port = 4321

received = []

server_thread = Thread.start do
  server = UDPSocket.open
  server.bind(nil, port)
  2.times { received << server.recvfrom(64) } 
  server.close
end

sleep 2 # Give thread a chance to be ready for input

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

test_ok(sock.close)

# test_exception(SocketError) { UDPSocket.open.send("BANG!", -1, 'invalid.', port) }

# Test UDPSocket using recv instead of recvfrom

# server thread echoes received data
server_thread = Thread.start do
  server_socket = UDPSocket.open
  server_socket.bind(nil, port)
  data, remote_info = server_socket.recvfrom(64)
  server_socket.send(data, 0, remote_info[3], remote_info[1])
  server_socket.close
end

sleep 2

client_socket = UDPSocket.open
client_socket.send("udp recv", 0, "localhost", port) 
server_thread.join
received = client_socket.recv(64)

test_equal("udp recv", received)
test_ok(client_socket.close)
  
# test that raising inside an accepting thread doesn't nuke the socket
accepted_latch = Java::java.util.concurrent.CountDownLatch.new(1)
closed_latch   = Java::java.util.concurrent.CountDownLatch.new(1)
tcp = TCPServer.new(nil, 5000)
exception = nil
t = Thread.new {
  begin
    accepted_latch.count_down
    tcp.accept
  rescue Exception => e
    exception = e
    # this would normally blow up if the socket was demolished
    tcp.close
    closed_latch.count_down
  end
}
accepted_latch.await
t.raise
closed_latch.await
test_ok RuntimeError === exception
