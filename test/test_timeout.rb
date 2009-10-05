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
      http = Net::HTTP.new('www.google.de')
      http.open_timeout = 0.01
      response = http.start do |h|
        h.request_get '/index.html'
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
    assert @in_foo.class.name == "", "Non-anonymous exception type raised in intervening stack"
  end

  # JRUBY-3928: Net::HTTP doesn't timeout as expected when using timeout.rb
  def test_timeout_socket_connect
    assert_raises(Timeout::Error) do
      timeout(0.1) do
        TCPSocket.new('google.com', 12345)
      end
    end
  end
end
