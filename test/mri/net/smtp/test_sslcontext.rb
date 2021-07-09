require 'net/smtp'
require 'test/unit'

unless defined?(OpenSSL::VERSION)
  warn "#{__FILE__}: openssl library not loaded; skipping"
  return
end

module Net
  class TestSSLContext < Test::Unit::TestCase
    # SERVER_CERT's subject has CN=localhost
    CA_FILE = File.expand_path("../fixtures/cacert.pem", __dir__)
    SERVER_KEY = File.expand_path("../fixtures/server.key", __dir__)
    SERVER_CERT = File.expand_path("../fixtures/server.crt", __dir__)

    class MySMTP < SMTP
      attr_reader :__ssl_context, :__ssl_socket

      def initialize(socket)
        @fake_socket = socket
        super("localhost")
      end

      def tcp_socket(*)
        @fake_socket
      end

      def ssl_socket(socket, context)
        @__ssl_context = context
        @__ssl_socket = super
      end
    end

    def teardown
      @server_thread&.exit&.join
      @server_socket&.close
      @client_socket&.close
    end

    private def default_ssl_context
      store = OpenSSL::X509::Store.new
      store.add_file(CA_FILE)
      SMTP.default_ssl_context(cert_store: store)
    end

    private def wrap_ssl_socket(sock)
      ctx = OpenSSL::SSL::SSLContext.new
      ctx.add_certificate(
        OpenSSL::X509::Certificate.new(File.read(SERVER_CERT)),
        OpenSSL::PKey.read(File.read(SERVER_KEY)),
        [OpenSSL::X509::Certificate.new(File.read(CA_FILE))])
      sock = OpenSSL::SSL::SSLSocket.new(sock, ctx)
      sock.sync_close = true
      sock.accept
    rescue OpenSSL::SSL::SSLError
      # The client must be raising SSLError, too
    end

    def start_smtpd_starttls
      @server_socket, @client_socket = Object.const_defined?(:UNIXSocket) ?
        UNIXSocket.pair : Socket.pair(:INET, :STREAM, 0)
      @server_thread = Thread.new(@server_socket) do |s|
        s.puts "220 fakeserver\r\n"
        while cmd = s.gets&.chomp
          case cmd
          when /\AEHLO /
            s.puts "250-fakeserver\r\n"
            s.puts "250-STARTTLS\r\n"
            s.puts "250 8BITMIME\r\n"
          when /\ARSET/
            s.puts "250 OK\r\n"
          when /\ASTARTTLS/
            s.puts "220 2.0.0 Ready to start TLS\r\n"
            s = wrap_ssl_socket(s) or break
          else
            raise "unsupported command: #{cmd}"
          end
        end
      end
      @client_socket
    end

    def start_smtpd_smtps
      @server_socket, @client_socket = Object.const_defined?(:UNIXSocket) ?
        UNIXSocket.pair : Socket.pair(:INET, :STREAM, 0)
      @server_thread = Thread.new(@server_socket) do |s|
        s = wrap_ssl_socket(s) or break
        s.puts "220 fakeserver\r\n"
        while cmd = s.gets&.chomp
          case cmd
          when /\AEHLO /
            s.puts "250-fakeserver\r\n"
            s.puts "250 8BITMIME\r\n"
          when /\ARSET/
            s.puts "250 OK\r\n"
          else
            raise "unsupported command: #{cmd}"
          end
        end
      end
      @client_socket
    end

    def test_default
      smtp = MySMTP.new(start_smtpd_starttls)
      assert_raise(OpenSSL::SSL::SSLError) { smtp.start }
      assert_equal(OpenSSL::X509::V_ERR_SELF_SIGNED_CERT_IN_CHAIN, smtp.__ssl_socket.verify_result)
      assert_equal(OpenSSL::SSL::VERIFY_PEER, smtp.__ssl_context.verify_mode)
    end

    def test_starttls_close_socket_on_verify_failure
      smtp = MySMTP.new(start_smtpd_starttls)
      assert_raise(OpenSSL::SSL::SSLError) { smtp.start }
      assert_equal(true, smtp.__ssl_socket.closed?)
    end

    def test_enable_tls
      smtp = MySMTP.new(start_smtpd_smtps)
      context = default_ssl_context
      smtp.enable_tls(context)
      smtp.start
      assert_equal(context, smtp.__ssl_context)
      assert_equal(true, smtp.rset.success?)
    end

    def test_enable_tls_before_disable_starttls
      smtp = MySMTP.new(start_smtpd_smtps)
      context = default_ssl_context
      smtp.enable_tls(context)
      smtp.disable_starttls
      smtp.start
      assert_equal(context, smtp.__ssl_context)
      assert_equal(true, smtp.rset.success?)
    end

    def test_enable_starttls
      smtp = MySMTP.new(start_smtpd_starttls)
      context = default_ssl_context
      smtp.enable_starttls(context)
      smtp.start
      assert_equal(context, smtp.__ssl_context)
      assert_equal(true, smtp.rset.success?)
    end

    def test_enable_starttls_before_disable_tls
      smtp = MySMTP.new(start_smtpd_starttls)
      context = default_ssl_context
      smtp.enable_starttls(context)
      smtp.disable_tls
      smtp.start
      assert_equal(context, smtp.__ssl_context)
      assert_equal(true, smtp.rset.success?)
    end

    def test_start_with_tls_verify_true
      smtp = MySMTP.new(start_smtpd_starttls)
      assert_raise(OpenSSL::SSL::SSLError) { smtp.start(tls_verify: true) }
      assert_equal(OpenSSL::X509::V_ERR_SELF_SIGNED_CERT_IN_CHAIN, smtp.__ssl_socket.verify_result)
      assert_equal(OpenSSL::SSL::VERIFY_PEER, smtp.__ssl_context.verify_mode)
    end

    def test_start_with_tls_verify_false
      smtp = MySMTP.new(start_smtpd_starttls)
      smtp.start(tls_verify: false)
      assert_equal(OpenSSL::SSL::VERIFY_NONE, smtp.__ssl_context.verify_mode)
      assert_equal(true, smtp.rset.success?)
    end

    def test_start_with_tls_hostname
      smtp = MySMTP.new(start_smtpd_starttls)
      context = default_ssl_context
      smtp.enable_starttls(context)
      assert_raise(OpenSSL::SSL::SSLError) { smtp.start(tls_hostname: "unexpected.example.com") }
      # TODO: Not all OpenSSL versions have the same verify_result code
      assert_equal("unexpected.example.com", smtp.__ssl_socket.hostname)
    end

    def test_start_without_tls_hostname
      smtp = MySMTP.new(start_smtpd_starttls)
      context = default_ssl_context
      smtp.enable_starttls(context)
      smtp.start
      assert_equal("localhost", smtp.__ssl_socket.hostname)
      assert_equal(true, smtp.rset.success?)
    end

    def test_start_with_ssl_context_params
      smtp = MySMTP.new(start_smtpd_starttls)
      smtp.start(ssl_context_params: {timeout: 123, verify_mode: OpenSSL::SSL::VERIFY_NONE})
      assert_equal(123, smtp.__ssl_context.timeout)
    end
  end
end
