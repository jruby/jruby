require 'test/unit'
require 'test/jruby/test_helper'

class TestTimeout < Test::Unit::TestCase
  include TestHelper

  def setup
    require 'timeout'
    require 'socket'
  end

  def test_timeout_for_loop
    n = 10000000
    assert_raises(Timeout::Error) do
      Timeout.timeout(0.1) { for i in 0..n do; (i + i % (i+1)) % (i + 10) ; end }
    end
  end

  def do_timeout(time, count, pass_expected, timeout_expected = 0, &block)
    pass = timeout = error = 0
    count.times do
      begin
        Timeout.timeout(time, &block)
        pass += 1
      rescue Timeout::Error => e
        timeout += 1
      rescue Timeout::ExitException => e
        error += 1
      end
    end
    assert_equal pass_expected, pass
    assert_equal timeout_expected, timeout
    assert_equal 0, error
  end

  # JRUBY-3743
  def test_subsecond_timeout_short_loop
    do_timeout(0.9999, 1000, 1000) { 1.times { |i| i } }
  end

  def test_subsecond_timeout_short_sleep
    do_timeout(0.9999, 1, 1) { sleep 0.1 }
  end

  def test_subsecond_timeout_long_sleep
    do_timeout(0.1, 1, 0, 1) { sleep 1 }
  end

  def test_timeout_sysread_socket
    port = rand(10000) + 5000
    server = TCPServer.new(port)
    client = TCPSocket.new('localhost', port)
    server.accept
    begin
      Timeout.timeout(0.1) { client.sysread(1024) }
    rescue Timeout::Error
      ok = true
    end

    assert ok, "IO#sysread was not interrupted by timeout"
  ensure
    begin; server.close; rescue Exception; end
    begin; client.close; rescue Exception; end
  end

  # JRUBY-3817
  false && def test_net_http_timeout
    require 'net/http'

    # Try binding to ports until we find one that's not already open
    server = nil
    port = 0
    1000.times do
      port = 10000 + rand(10000)
      begin
        server = ServerSocket.new(:INET, :STREAM)
        server.bind(Addrinfo.tcp("127.0.0.1", port))
        break
      rescue
        server = nil
        next
      end
    end

    fail "could not find an open port" unless server

    http = Net::HTTP.new('127.0.0.1', port)
    http.open_timeout = 0.01

    assert_raises Net::OpenTimeout do
      http.start {}
    end
  ensure
    server.close rescue nil
  end

  def test_timeout_raises_anon_class_to_unroll
    begin
      quiet do
        Timeout.timeout(0.1) { foo }
      end
    rescue Timeout::Error
      ok = true
    end

    assert ok, "Timeout::Error was not eventually delivered to caller"
  end

  def foo
    sleep 2
  rescue Exception => e
    @in_foo = e
    raise e
  end

  # JRUBY-3928: Net::HTTP doesn't timeout as expected when using timeout.rb
  def test_timeout_socket_connect
    assert_raises(Timeout::Error) do
      Timeout.timeout(0.1) do
        TCPSocket.new('google.com', 12345)
      end
    end
  end

  # JRUBY-5099: Built-in timeout method added to wrong class
  def test_timeout_toplevel_method
    cls = Class.new do
      def method_missing(name, *args)
        if name == :timeout
          42
        end
      end
    end
    assert_equal 42, cls.new.timeout, "timeout should have returned 42"
  end

  class Seconds

    attr_reader :value

    def initialize(value); @value = value end

    def self.===(other); other.is_a?(Seconds) end

    def ==(other)
      if Seconds === other
        other.value == value
      else
        other == value
      end
    end

    def eql?(other); other.is_a?(Seconds) && self == other end

    def divmod(divisor)
      value.divmod(divisor)
    end

    private

    def method_missing(method, *args, &block)
      value.send(method, *args, &block)
    end

  end

  def test_timeout_interval_argument
    assert_equal 42, Timeout.timeout(Seconds.new(2)) { 42 }
    assert_raises(Timeout::Error) do
      Timeout.timeout(Seconds.new(0.3)) { sleep(0.5) }
    end
  end

end
