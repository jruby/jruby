require 'socket'

module SocketSpecs
  # helper to get the hostname associated to 127.0.0.1
  def self.hostname
    # Calculate each time, without caching, since the result might
    # depend on things like do_not_reverse_lookup mode, which is
    # changing from test to test
    Socket.getaddrinfo("127.0.0.1", nil)[0][2]
  end

  def self.hostnamev6
    Socket.getaddrinfo("::1", nil)[0][2]
  end

  def self.addr(which=:ipv4)
    case which
    when :ipv4
      host = "127.0.0.1"
    when :ipv6
      host = "::1"
    end
    Socket.getaddrinfo(host, nil)[0][3]
  end

  def self.port
    @@_port ||= begin
                  s = TCPServer.open(0)
                  port = s.addr[1]
                  s.close
                  port
                rescue
                  43191
                end
  end

  def self.str_port
    port.to_s
  end

  def self.local_port
    @base ||= $$
    @base += 1
    local_port = (@base % (0xffff-1024)) + 1024
    local_port += 1 if local_port == port
    local_port
  end

  def self.sockaddr_in(port, host)
    Socket::SockAddr_In.new(Socket.sockaddr_in(port, host))
  end

  def self.socket_path
    tmp("unix_server_spec.socket", false)
  end

  # TCPServer that does not block waiting for connections. Each
  # connection is serviced in a separate thread. The data read
  # from the socket is echoed back. The server is shutdown when
  # the spec process exits.
  class SpecTCPServer
    def self.start(host=nil, port=nil, logger=nil)
      return if @server

      @server = new host, port, logger
      @server.start

      at_exit do
        @server.shutdown
      end
    end

    def self.get
      @server
    end

    attr_accessor :hostname, :port, :logger

    def initialize(host=nil, port=nil, logger=nil)
      @hostname = host || SocketSpecs.hostname
      @port = port || SocketSpecs.port
      @logger = logger
      @shutdown = false
      @accepted = false
      @main = nil
      @server = nil
      @threads = []
    end

    def start
      @main = Thread.new do
        log "SpecTCPServer starting on #{@hostname}:#{@port}"
        @server = TCPServer.new @hostname, @port

        wait_for @server do
          socket = @server.accept
          log "SpecTCPServer accepted connection: #{socket}"
          service socket

          @accepted = true
        end
      end

      Thread.pass until @server
    end

    def service(socket)
      thr = Thread.new do
        begin
          wait_for socket do
            data = socket.recv(1024)
            break if data.empty?
            log "SpecTCPServer received: #{data.inspect}"

            break if data == "QUIT"

            socket.send data, 0
          end
        ensure
          socket.close
        end
      end

      @threads << thr
    end

    def wait_for(io)
      loop do
        read, _, _ = IO.select([io], [], [], 0.25)
        return false if shutdown?
        yield if read
      end
    end

    def shutdown?
      @shutdown
    end

    def shutdown
      @shutdown = true
      log "SpecTCPServer shutting down"

      @threads.each { |thr| thr.join }
      @main.join
      @server.close if @accepted
    end

    def log(message)
      @logger.puts message if @logger
    end
  end
end
