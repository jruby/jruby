# frozen_string_literal: true

# Kestówv 0.5.1 — ipc/server.rb
#
# IPC server — listens on IPC_ADDR and accepts Channel connections.
# Follows JEP-380 + framing + three-tier fallback patterns from channel.rb.

module Kestowv
  module Ipc
    class Server

      BACKLOG = 128   # listen queue depth

      attr_reader :addr, :tier

      # --------------------------------------------------------
      # LIFECYCLE
      # --------------------------------------------------------

      def initialize(addr: Channel::IPC_ADDR,
                     tcp_host: Channel::TCP_HOST,
                     tcp_port: Channel::TCP_PORT)
        @addr      = addr
        @tcp_host  = tcp_host
        @tcp_port  = tcp_port
        @server    = nil
        @tier      = nil
        @listening = false
        @mutex     = Mutex.new
        @stats     = { accepted: 0, errors: 0 }
      end

      def listen
        Channel::TIERS.each do |t|
          result = try_listen(t)
          if result
            @tier      = t
            @listening = true
            Boot.register(server_bit)
            Boot.set_bit(server_bit)
            warn "[IPC::Server] Listening on #{@addr} via #{@tier}" unless Boot.config.quiet
            return self
          end
        end

        raise Channel::ConnectionError, "All IPC server tiers exhausted for #{@addr}"
      end

      def listening?
        @mutex.synchronize { @listening }
      end

      # Accept one incoming connection — returns a Channel.
      # Blocks until a client connects or the server is closed.
      def accept
        raise Channel::NotConnectedError, "Server not listening" unless listening?

        raw = accept_raw
        return nil unless raw

        @mutex.synchronize { @stats[:accepted] += 1 }
        Channel.from_socket(raw, tier: @tier)
      rescue => e
        @mutex.synchronize { @stats[:errors] += 1 }
        Boot.handle_error(e, { component: :ipc_server, tier: @tier })
        nil
      end

      # Accept in a loop, yielding each Channel to the block.
      # Runs until the server is closed or the block raises StopIteration.
      def accept_loop(&block)
        raise ArgumentError, "Block required" unless block

        while listening?
          ch = accept
          next unless ch
          block.call(ch)
        end
      rescue StopIteration
        # graceful exit
      end

      def close
        @mutex.synchronize do
          return self unless @listening
          @server&.close rescue nil
          @server    = nil
          @listening = false
        end

        # Clean up socket file on Unix
        if @tier != :tcp && !Channel::WINDOWS
          File.delete(@addr) rescue nil
        end

        Boot.clear_bit(server_bit)
        self
      end

      # --------------------------------------------------------
      # INTROSPECTION
      # --------------------------------------------------------

      def stats
        @mutex.synchronize { @stats.dup }
      end

      def to_h
        {
          addr:      @addr,
          tier:      @tier,
          listening: @listening,
          stats:     stats
        }
      end

      def to_s
        "#<IPC::Server addr=#{@addr} tier=#{@tier} listening=#{@listening}>"
      end

      private

      # --------------------------------------------------------
      # TIER LISTEN ATTEMPTS
      # --------------------------------------------------------

      def try_listen(tier)
        case tier
        when :jep380       then listen_jep380
        when :abstract_uds then listen_abstract_uds
        when :tcp          then listen_tcp
        end
      rescue => e
        warn "[IPC::Server] #{tier} listen failed: #{e.message}" unless Boot.config.quiet
        nil
      end

      def listen_jep380
        # Remove stale socket file before binding
        File.delete(@addr) rescue nil

        addr = java.net.UnixDomainSocketAddress.of(@addr)
        ch   = java.nio.channels.ServerSocketChannel.open(
          java.net.StandardProtocolFamily::UNIX
        )
        ch.bind(addr, BACKLOG)
        ch.configureBlocking(true)
        @server = ch
        true
      end

      def listen_abstract_uds
        require "socket"
        File.delete(@addr) rescue nil
        @server = UNIXServer.new(@addr)
        true
      rescue
        nil
      end

      def listen_tcp
        require "socket"
        @server = TCPServer.new(@tcp_host, @tcp_port)
        true
      end

      # --------------------------------------------------------
      # RAW ACCEPT — tier-specific
      # --------------------------------------------------------

      def accept_raw
        case @server
        when java.nio.channels.ServerSocketChannel
          ch = @server.accept
          return nil unless ch
          ch.configureBlocking(true)
          ch
        else
          @server.accept
        end
      end

      def server_bit
        :"ipc_server_#{@tier}"
      end
    end
  end
end

# --------------------------------------------------------
# Channel.from_socket — package-private factory
# Allows Server to hand a raw socket to Channel without
# using instance_variable_set.
# --------------------------------------------------------
module Kestowv
  module Ipc
    class Channel
      def self.from_socket(raw_socket, tier:)
        ch = allocate
        ch.send(:init_from_socket, raw_socket, tier)
        ch
      end

      private

      def init_from_socket(raw_socket, tier)
        @socket    = raw_socket
        @tier      = tier
        @connected = true
        @mutex     = Mutex.new
        @stats     = { sent: 0, received: 0, errors: 0, bytes_sent: 0, bytes_received: 0 }
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :ipc_server,
  __FILE__,
  feature:    :ipc_server,
  depends_on: [:ipc_channel]
)
