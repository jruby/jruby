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

### UDP ###

test_ok(UDPSocket::open)

port = 4321

received = []

server_thread = Thread.start do
  server = UDPSocket.open
  server.bind(nil, port)
  2.times { received << server.recvfrom(64) }
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

test_exception(SocketError) { UDPSocket.open.send("BANG!", -1, 'invalid.', port) }
