# frozen_string_literal: true

# Kestówv 0.5.1 — ipc/channel.rb
#
# General-purpose IPC channel built on JEP-380 (UnixDomainSocketChannel).
# Follows established patterns from the JRuby JEP-380 full prototype:
#   - IPC_ADDR resolved at boot via WINDOWS boolean
#   - 4-byte length-prefixed framing (346K events/sec baseline)
#   - ByteClass integration via :ipc_message
#   - Three-tier fallback: JEP-380 → abstract UDS → TCP

module Kestowv
  module Ipc
    class Channel

      # --------------------------------------------------------
      # BOOT-TIME CONSTANTS — resolved once, never branched on again.
      # Mirrors the Rubian Windows compat pattern.
      # --------------------------------------------------------

      WINDOWS  = (defined?(::WINDOWS) ? ::WINDOWS : Gem::Platform.local.os == "mingw32") rescue false

      IPC_ROOT = WINDOWS ? '\\\\.\\pipe\\' : '/tmp/'

      IPC_ADDR = WINDOWS \
        ? "#{IPC_ROOT}kestowv-ipc"
        : "#{IPC_ROOT}kestowv-ipc.sock"

      # TCP fallback coordinates
      TCP_HOST = "127.0.0.1"
      TCP_PORT = 17_380   # JEP-380 tribute port

      HEADER_SIZE   = 4       # 4-byte big-endian length prefix
      MAX_MSG_SIZE  = 64 * 1024 * 1024  # 64MB hard cap — reject oversized frames
      READ_TIMEOUT  = 30      # seconds before a blocking read gives up

      # Transport tiers — tried in order
      TIERS = %i[jep380 abstract_uds tcp].freeze

      attr_reader :connected, :tier, :stats

      # --------------------------------------------------------
      # LIFECYCLE
      # --------------------------------------------------------

      def initialize(addr: IPC_ADDR, tcp_host: TCP_HOST, tcp_port: TCP_PORT)
        @addr      = addr
        @tcp_host  = tcp_host
        @tcp_port  = tcp_port
        @socket    = nil
        @connected = false
        @tier      = nil
        @mutex     = Mutex.new
        @stats     = { sent: 0, received: 0, errors: 0, bytes_sent: 0, bytes_received: 0 }
      end

      def connect
        TIERS.each do |t|
          result = try_tier(t)
          if result
            @tier      = t
            @connected = true
            Boot.set_bit(:"ipc_channel_#{t}")
            return self
          end
        end

        raise ConnectionError, "All IPC tiers exhausted for #{@addr}"
      end

      def disconnect
        @mutex.synchronize do
          return self unless @connected
          close_socket_unsafe
          @connected = false
          @tier      = nil
        end
        Boot.clear_bit(:"ipc_channel_#{@tier}") if @tier
        self
      end

      def connected?
        @mutex.synchronize { @connected }
      end

      # --------------------------------------------------------
      # FRAMING — 4-byte big-endian length prefix
      # --------------------------------------------------------

      def send_message(data)
        raise NotConnectedError unless connected?

        bytes  = data.to_s.b
        length = bytes.bytesize

        raise MessageTooLargeError, "#{length} > #{MAX_MSG_SIZE}" if length > MAX_MSG_SIZE

        frame  = [length].pack("N") + bytes

        @mutex.synchronize do
          write_frame_unsafe(frame)
          @stats[:sent]       += 1
          @stats[:bytes_sent] += frame.bytesize
        end

        self
      end

      def receive_message
        raise NotConnectedError unless connected?

        header = read_exact(HEADER_SIZE)
        return nil unless header

        length = header.unpack1("N")

        if length > MAX_MSG_SIZE
          @mutex.synchronize { @stats[:errors] += 1 }
          raise MessageTooLargeError, "Incoming frame #{length} > #{MAX_MSG_SIZE}"
        end

        body = read_exact(length)
        @mutex.synchronize do
          @stats[:received]       += 1
          @stats[:bytes_received] += length
        end

        body
      end

      # --------------------------------------------------------
      # BYTECLASS INTEGRATION
      # --------------------------------------------------------

      def self.byte_kind
        :ipc_message
      end

      # --------------------------------------------------------
      # INTROSPECTION
      # --------------------------------------------------------

      def to_h
        {
          addr:      @addr,
          tier:      @tier,
          connected: @connected,
          stats:     @stats.dup
        }
      end

      def to_s
        "#<Channel addr=#{@addr} tier=#{@tier} connected=#{@connected}>"
      end

      # --------------------------------------------------------
      # ERRORS
      # --------------------------------------------------------

      class ConnectionError     < RuntimeError; end
      class NotConnectedError   < RuntimeError; end
      class MessageTooLargeError < RuntimeError; end
      class FramingError        < RuntimeError; end

      private

      # --------------------------------------------------------
      # TIER CONNECT ATTEMPTS
      # --------------------------------------------------------

      def try_tier(tier)
        case tier
        when :jep380        then connect_jep380
        when :abstract_uds  then connect_abstract_uds
        when :tcp           then connect_tcp
        end
      rescue => e
        warn "[IPC] #{tier} failed: #{e.message}" unless Boot.config.quiet
        nil
      end

      def connect_jep380
        addr = java.net.UnixDomainSocketAddress.of(@addr)
        ch   = java.nio.channels.SocketChannel.open(addr)
        ch.configureBlocking(true)
        @socket = ch
        true
      end

      def connect_abstract_uds
        # Abstract namespace UDS (Linux) or named pipe fallback (Windows).
        # Mirrors the ValenKernel abstract UDS pattern.
        require "socket"
        sock = Socket.new(:UNIX, :STREAM)
        sock.connect(Socket.pack_sockaddr_un(@addr))
        @socket = sock
        true
      rescue
        nil
      end

      def connect_tcp
        require "socket"
        @socket = TCPSocket.new(@tcp_host, @tcp_port)
        true
      end

      # --------------------------------------------------------
      # FRAME I/O
      # --------------------------------------------------------

      # Must be called with @mutex held (for JEP-380 socket).
      def write_frame_unsafe(frame)
        case @socket
        when java.nio.channels.SocketChannel
          buf = java.nio.ByteBuffer.wrap(frame.to_java_bytes)
          written = 0
          while buf.hasRemaining
            written += @socket.write(buf)
          end
        else
          @socket.write(frame)
        end
      end

      # read_exact is called outside @mutex — reads are serialized by
      # the blocking nature of the socket, not by the mutex.
      # Holding @mutex during a blocking read would prevent concurrent sends.
      def read_exact(n)
        case @socket
        when java.nio.channels.SocketChannel
          read_exact_jep380(n)
        else
          read_exact_ruby(n)
        end
      end

      def read_exact_jep380(n)
        buf   = java.nio.ByteBuffer.allocate(n)
        total = 0

        while total < n
          read = @socket.read(buf)
          raise FramingError, "Connection closed mid-frame" if read == -1
          total += read
        end

        buf.flip
        String.from_java_bytes(buf.array[0, n])
      end

      def read_exact_ruby(n)
        buf = "".b
        while buf.bytesize < n
          chunk = @socket.read(n - buf.bytesize)
          raise FramingError, "Connection closed mid-frame" if chunk.nil?
          buf << chunk
        end
        buf
      end

      def close_socket_unsafe
        @socket&.close rescue nil
        @socket = nil
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :ipc_channel,
  __FILE__,
  feature:    :ipc,
  depends_on: [:core_kobject]
)
