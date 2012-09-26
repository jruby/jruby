require 'test/unit'
require 'timeout'
require 'socket'
require 'net/http'

class TestTimeout < Test::Unit::TestCase
  def test_timeout_for_loop
    n = 10000000
    assert_raises(Timeout::Error) do
      Timeout::timeout(1) { for i in 0..n do; (i + i % (i+1)) % (i + 10) ; end }
    end
  end

  def do_timeout(time, count, pass_expected, timeout_expected = 0, &block)
    pass = timeout = error = 0
    count.times do |i|
      begin
        Timeout::timeout(time, &block)
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
      timeout(0.1) { client.sysread(1024) }
    rescue Timeout::Error
      ok = true
    end

    assert ok, "IO#sysread was not interrupted by timeout"
  ensure
    begin; server.close; rescue Exception; end
    begin; client.close; rescue Exception; end
  end

  def foo
    sleep 5
  rescue Exception => e
    @in_foo = e
    raise e
  end

  # JRUBY-3817
  def test_net_http_timeout
    assert_raises Timeout::Error do
      http = Net::HTTP.new('8.8.8.8')
      http.open_timeout = 0.001
      response = http.start do |h|
        h.request_get '/index.html'
        # ensure we timeout even if we're fast
        sleep(0.01)
      end
    end
  end

  def test_timeout_raises_anon_class_to_unroll
    begin
      timeout(0.1) { foo }
    rescue Timeout::Error
      ok = true
    end

    assert ok, "Timeout::Error was not eventually delivered to caller"
    if RUBY_VERSION =~ /1\.8/ # FIXME is this ok?
      assert @in_foo.class.name == "", "Non-anonymous exception type raised in intervening stack"
    end
  end

  # JRUBY-3928: Net::HTTP doesn't timeout as expected when using timeout.rb
  def test_timeout_socket_connect
    assert_raises(Timeout::Error) do
      timeout(0.1) do
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
    assert cls.new.timeout, "timeout should have returned 42"
  end

  # GH-312: Nested timeouts trigger inner for outer's timeout
  def test_nested_timeout
    result = []
    expected = [
      'Timeout 2: Non-timeout exception',
      'Timeout 2: ensure',
      'Timeout 1: triggered',
      'Timeout 1: ensure'
    ]

    begin
      Timeout.timeout(1) do
	begin
	  Timeout.timeout(2) do
	    sleep(5)
	  end
	rescue Timeout::Error
	  result << 'Timeout 2: triggered'
          raise
        rescue Exception
          result << 'Timeout 2: Non-timeout exception'
          raise
	ensure
	  result << 'Timeout 2: ensure'
	end
      end
    rescue Timeout::Error
      result << 'Timeout 1: triggered'
    rescue Exception
      result << 'Timeout 1: Non-timeout exception'
    ensure
      result << 'Timeout 1: ensure'
    end

    assert_equal expected, result
  end
end
