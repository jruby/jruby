require 'test/minirunit.rb'
require 'socket'

UNUSED_PORT = 9999

def testServer
  port = 7777
  serverMessages = []
  serverThread = Thread.new {
    begin
      server = TCPServer.new(nil, port);
      while (session = server.accept) 
        msg = session.gets
        serverMessages << msg;
        if msg =~ /^quit/
          session.close
          test_exception(Errno::EBADF) {
            session.close
          }
          session = nil
          break;
        end
        session.puts msg
      end
    rescue => e
      $stderr.puts e,e.backtrace
    ensure
      session.close if session
      server.close()
    end
  }
  
  count = 0
  begin
    clientSocket = TCPSocket.new('localhost', port)
    clientSocket.puts "quit"
    
    test_ok nil != serverThread.join, "Server Thread did not end"
    test_equal(["quit\n"], serverMessages)
    return nil
  rescue Errno::ECONNREFUSED
    count += 1
    sleep 1
    retry unless count > 1
  end
  
  $stderr.puts "Waiting for server Thread"
  test_fail("client could not connect") 
  sleep 5
end

def testNoConnection
  test_exception(Errno::ECONNREFUSED) {
    clientSocket = TCPSocket.new('localhost', UNUSED_PORT)
  }
end

TEST_LINE = "this is a test\n"

def testSimpleEcho
  clientSocket = TCPSocket.new('localhost', 7)
  clientSocket.puts TEST_LINE
  
  line = clientSocket.gets
  test_equal(TEST_LINE, line)
  clientSocket.close
end

testNoConnection
testSimpleEcho
testServer
