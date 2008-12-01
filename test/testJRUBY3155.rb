require 'io/wait'
require 'socket'
require 'thread'
require 'test/minirunit'

# See http://jira.codehaus.org/browse/JRUBY-3155
test_check "JRUBY-3155:"

listenIp = "0.0.0.0"
listenPort = 12345

server = TCPServer.new(listenIp,listenPort)
server.setsockopt(Socket::IPPROTO_TCP, Socket::TCP_NODELAY, true)

server_strings = []
client_strings = []

server_thread = Thread.new do
  begin
    thisThread = Thread.new(session = server.accept) do |thisSession|
      5.times do |i|
        fromClient = thisSession.gets
        server_strings << fromClient
        thisSession.write(fromClient)
      end
    end
  rescue StandardError => bang
    raise
  end
end

Thread.pass until server_thread.status == 'sleep'

s = TCPSocket.new 'localhost', listenPort

receive_thread = Thread.start do
  begin
    5.times do |i|
      str = s.gets
      client_strings << str
    end
  rescue Object
  end
end

5.times do |i|
  s.puts i.to_s
end

receive_thread.join
server_thread.join

test_equal ["0\n", "1\n", "2\n", "3\n", "4\n"], server_strings
test_equal ["0\n", "1\n", "2\n", "3\n", "4\n"], client_strings