# frozen_string_literal: true
require 'net/smtp'
require 'stringio'
require 'test/unit'

module Net
  class TestSMTP < Test::Unit::TestCase
    CA_FILE = File.expand_path("../fixtures/cacert.pem", __dir__)
    SERVER_KEY = File.expand_path("../fixtures/server.key", __dir__)
    SERVER_CERT = File.expand_path("../fixtures/server.crt", __dir__)

    class FakeSocket
      attr_reader :write_io

      def initialize out = "250 OK\n"
        @write_io = StringIO.new
        @read_io  = StringIO.new out
      end

      def writeline line
        @write_io.write "#{line}\r\n"
      end

      def readline
        line = @read_io.gets
        raise 'ran out of input' unless line
        line.chop
      end
    end

    def setup
      # Avoid hanging at fake_server_start's IO.select on --jit-wait CI like http://ci.rvm.jp/results/trunk-mjit-wait@phosphorus-docker/3302796
      # Unfortunately there's no way to configure read_timeout for Net::SMTP.start.
      if defined?(RubyVM::JIT) && RubyVM::JIT.enabled?
        Net::SMTP.prepend Module.new {
          def initialize(*)
            super
            @read_timeout *= 5
          end
        }
      end

      @server_threads = []
    end

    def teardown
      @server_threads.each {|th| th.join }
    end

    def test_critical
      smtp = Net::SMTP.new 'localhost', 25

      assert_raise RuntimeError do
        smtp.send :critical do
          raise 'fail on purpose'
        end
      end

      assert_kind_of Net::SMTP::Response, smtp.send(:critical),
                     '[Bug #9125]'
    end

    def test_esmtp
      smtp = Net::SMTP.new 'localhost', 25
      assert smtp.esmtp
      assert smtp.esmtp?

      smtp.esmtp = 'omg'
      assert_equal 'omg', smtp.esmtp
      assert_equal 'omg', smtp.esmtp?
    end

    def test_rset
      smtp = Net::SMTP.new 'localhost', 25
      smtp.instance_variable_set :@socket, FakeSocket.new

      assert smtp.rset
    end

    def test_mailfrom
      sock = FakeSocket.new
      smtp = Net::SMTP.new 'localhost', 25
      smtp.instance_variable_set :@socket, sock
      assert smtp.mailfrom("foo@example.com").success?
      assert_equal "MAIL FROM:<foo@example.com>\r\n", sock.write_io.string
    end

    def test_rcptto
      sock = FakeSocket.new
      smtp = Net::SMTP.new 'localhost', 25
      smtp.instance_variable_set :@socket, sock
      assert smtp.rcptto("foo@example.com").success?
      assert_equal "RCPT TO:<foo@example.com>\r\n", sock.write_io.string
    end

    def test_auth_plain
      sock = FakeSocket.new
      smtp = Net::SMTP.new 'localhost', 25
      smtp.instance_variable_set :@socket, sock
      assert smtp.auth_plain("foo", "bar").success?
      assert_equal "AUTH PLAIN AGZvbwBiYXI=\r\n", sock.write_io.string
    end

    def test_crlf_injection
      smtp = Net::SMTP.new 'localhost', 25
      smtp.instance_variable_set :@socket, FakeSocket.new

      assert_raise(ArgumentError) do
        smtp.mailfrom("foo\r\nbar")
      end

      assert_raise(ArgumentError) do
        smtp.mailfrom("foo\rbar")
      end

      assert_raise(ArgumentError) do
        smtp.mailfrom("foo\nbar")
      end

      assert_raise(ArgumentError) do
        smtp.rcptto("foo\r\nbar")
      end
    end

    def test_tls_connect
      omit "openssl library not loaded" unless defined?(OpenSSL::VERSION)

      servers = Socket.tcp_server_sockets("localhost", 0)
      ctx = OpenSSL::SSL::SSLContext.new
      ctx.ca_file = CA_FILE
      ctx.key = File.open(SERVER_KEY) { |f|
        OpenSSL::PKey::RSA.new(f)
      }
      ctx.cert = File.open(SERVER_CERT) { |f|
        OpenSSL::X509::Certificate.new(f)
      }
      sock = nil
      Thread.start do
        s = accept(servers)
        sock = OpenSSL::SSL::SSLSocket.new(s, ctx)
        sock.sync_close = true
        sock.accept
        sock.write("220 localhost Service ready\r\n")
        sock.gets
        sock.write("250 localhost\r\n")
        sock.gets
        sock.write("221 localhost Service closing transmission channel\r\n")
      end
      smtp = Net::SMTP.new("localhost", servers[0].local_address.ip_port)
      smtp.enable_tls
      smtp.open_timeout = 1
      smtp.start(tls_verify: false) do
      end
    ensure
      sock&.close
      servers&.each(&:close)
    end

    def test_tls_connect_timeout
      omit "openssl library not loaded" unless defined?(OpenSSL::VERSION)

      servers = Socket.tcp_server_sockets("localhost", 0)
      sock = nil
      Thread.start do
        sock = accept(servers)
      end
      smtp = Net::SMTP.new("localhost", servers[0].local_address.ip_port)
      smtp.enable_tls
      smtp.open_timeout = 0.1
      assert_raise(Net::OpenTimeout) do
        smtp.start do
        end
      end
    ensure
      sock&.close
      servers&.each(&:close)
    end

    def test_eof_error_backtrace
      bug13018 = '[ruby-core:78550] [Bug #13018]'
      servers = Socket.tcp_server_sockets("localhost", 0)
      begin
        sock = nil
        t = Thread.start do
          sock = accept(servers)
          sock.close
        end
        smtp = Net::SMTP.new("localhost", servers[0].local_address.ip_port)
        e = assert_raise(EOFError, bug13018) do
          smtp.start do
          end
        end
        assert_equal(EOFError, e.class, bug13018)
        assert(e.backtrace.grep(%r"\bnet/smtp\.rb:").size > 0, bug13018)
      ensure
        sock.close if sock
        servers.each(&:close)
        t.join
      end
    end

    def test_start
      port = fake_server_start
      smtp = Net::SMTP.start('localhost', port)
      smtp.finish
    end

    def test_start_with_position_argument
      port = fake_server_start(helo: 'myname', user: 'account', password: 'password')
      smtp = Net::SMTP.start('localhost', port, 'myname', 'account', 'password', :plain)
      smtp.finish
    end

    def test_start_with_keyword_argument
      port = fake_server_start(helo: 'myname', user: 'account', password: 'password')
      smtp = Net::SMTP.start('localhost', port, helo: 'myname', user: 'account', secret: 'password', authtype: :plain)
      smtp.finish
    end

    def test_start_password_is_secret
      port = fake_server_start(helo: 'myname', user: 'account', password: 'password')
      smtp = Net::SMTP.start('localhost', port, helo: 'myname', user: 'account', password: 'password', authtype: :plain)
      smtp.finish
    end

    def test_start_invalid_number_of_arguments
      err = assert_raise ArgumentError do
        Net::SMTP.start('localhost', 25, 'myname', 'account', 'password', :plain, :invalid_arg)
      end
      assert_equal('wrong number of arguments (given 7, expected 1..6)', err.message)
    end

    def test_start_instance
      port = fake_server_start
      smtp = Net::SMTP.new('localhost', port)
      smtp.start
      smtp.finish
    end

    def test_start_instance_with_position_argument
      port = fake_server_start(helo: 'myname', user: 'account', password: 'password')
      smtp = Net::SMTP.new('localhost', port)
      smtp.start('myname', 'account', 'password', :plain)
      smtp.finish
    end

    def test_start_instance_with_keyword_argument
      port = fake_server_start(helo: 'myname', user: 'account', password: 'password')
      smtp = Net::SMTP.new('localhost', port)
      smtp.start(helo: 'myname', user: 'account', secret: 'password', authtype: :plain)
      smtp.finish
    end

    def test_start_instance_password_is_secret
      port = fake_server_start(helo: 'myname', user: 'account', password: 'password')
      smtp = Net::SMTP.new('localhost', port)
      smtp.start(helo: 'myname', user: 'account', password: 'password', authtype: :plain)
      smtp.finish
    end

    def test_start_instance_invalid_number_of_arguments
      smtp = Net::SMTP.new('localhost')
      err = assert_raise ArgumentError do
        smtp.start('myname', 'account', 'password', :plain, :invalid_arg)
      end
      assert_equal('wrong number of arguments (given 5, expected 0..4)', err.message)
    end

    private

    def accept(servers)
      Socket.accept_loop(servers) { |s, _| break s }
    end

    def fake_server_start(helo: 'localhost', user: nil, password: nil)
      servers = Socket.tcp_server_sockets('localhost', 0)
      @server_threads << Thread.start do
        Thread.current.abort_on_exception = true
        sock = accept(servers)
        sock.puts "220 ready\r\n"
        assert_equal("EHLO #{helo}\r\n", sock.gets)
        sock.puts "220-servername\r\n220 AUTH PLAIN\r\n"
        if user
          credential = ["\0#{user}\0#{password}"].pack('m0')
          assert_equal("AUTH PLAIN #{credential}\r\n", sock.gets)
          sock.puts "235 2.7.0 Authentication successful\r\n"
        end
        assert_equal("QUIT\r\n", sock.gets)
        sock.puts "221 2.0.0 Bye\r\n"
        sock.close
        servers.each(&:close)
      end
      port = servers[0].local_address.ip_port
      return port
    end
  end
end
