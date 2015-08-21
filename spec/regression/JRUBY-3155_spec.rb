require 'rspec'
require 'socket'

# Disabled because it doesn't pass in MRI 2.1.1 either See GH#1909
false && describe('JRUBY-3155') do
  it 'passes' do
    # See http://jira.codehaus.org/browse/JRUBY-3155
    listenIp = "0.0.0.0"
    listenPort = 12345

    begin
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

      begin
        5.times do |i|
          str = s.gets
          client_strings << str
        end
      rescue Object
      end

      5.times do |i|
        s.puts i.to_s
      end

      server_thread.join

      expect(server_strings).to eq(["0\n", "1\n", "2\n", "3\n", "4\n"])
      expect(client_strings).to eq(["0\n", "1\n", "2\n", "3\n", "4\n"])
    ensure
      server.close rescue nil
      s.close rescue nil
    end
  end
end
