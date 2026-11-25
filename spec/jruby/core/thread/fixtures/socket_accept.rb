require 'java'
require 'socket'
require 'rspec/autorun'

virtual = ARGV.shift == 'true'

class CancelledAccept < StandardError; end

RSpec.describe "Concurrent socket accepts with thread.vthread=#{virtual}" do
  before do
    @virtual = virtual
    @server = TCPServer.new('127.0.0.1', 0)
    @port = @server.addr[1]
    @threads = []
  end

  after do
    @server.close unless @server.closed?
    @threads.each(&:kill)
    @threads.each(&:join)
  end

  def acceptor(ready, virtual)
    @threads << Thread.new do
      expect(java.lang.Thread.current_thread.virtual?).to eq(virtual)
      ready << true
      socket = @server.accept
      begin
        socket.gets.chomp
      ensure
        socket.close
      end
    rescue CancelledAccept
      :cancelled
    end
    @threads.last
  end

  def client(message)
    @threads << Thread.new do
      expect(java.lang.Thread.current_thread.virtual?).to eq(@virtual)
      TCPSocket.open('127.0.0.1', @port) { |socket| socket.puts(message) }
      :sent
    end
    @threads.last
  end

  it 'runs clients while multiple threads wait to accept on the same server' do
    ready = Queue.new
    acceptors = 2.times.map { acceptor(ready, virtual) }
    2.times { ready.pop }
    clients = ['first', 'second'].map { |message| client(message) }
    expect(clients.map(&:value)).to eq([:sent, :sent])
    expect(acceptors.map(&:value).sort).to eq(['first', 'second'])
  end

  it 'interrupts one acceptor while another continues and permits later accepts' do
    ready = Queue.new
    interrupted = acceptor(ready, virtual)
    remaining = acceptor(ready, virtual)
    2.times { ready.pop }
    Thread.pass until interrupted.stop?
    interrupted.raise(CancelledAccept)
    expect(interrupted.value).to eq(:cancelled)
    expect(client('remaining').value).to eq(:sent)
    expect(remaining.value).to eq('remaining')

    later = acceptor(ready, virtual)
    ready.pop
    expect(client('later').value).to eq(:sent)
    expect(later.value).to eq('later')
  end

  it 'interrupts all pending acceptors when the server closes' do
    ready = Queue.new
    acceptors = 2.times.map do
      @threads << Thread.new do
        expect(java.lang.Thread.current_thread.virtual?).to eq(virtual)
        ready << true
        @server.accept
      rescue IOError
        :closed
      end
      @threads.last
    end
    2.times { ready.pop }
    Thread.pass until acceptors.all?(&:stop?)
    expect(acceptors.map(&:alive?)).to eq([true, true])
    @server.close
    expect(acceptors.map(&:value)).to eq([:closed, :closed])
  end
end
