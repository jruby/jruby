require 'test/unit'
require 'timeout'
require 'socket'

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
  ensure
    begin; server.close; rescue Exception; end
    begin; client.close; rescue Exception; end
  end
end
