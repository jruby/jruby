require 'openssl'
require 'test/unit'
require 'webrick/https'
require 'net/https'
require 'logger'
require File.join(File.dirname(__FILE__), (RUBY_VERSION >= '1.9.0' ? '1.9' : '1.8'), "utils.rb")


class TestSSL < Test::Unit::TestCase
  PORT = 17171
  DIR = File.dirname(File.expand_path(__FILE__))

  def setup
    @server = @server_thread = nil
    @verbose, $VERBOSE = $VERBOSE, nil
    setup_server
  end

  def teardown
    $VERBOSE = @verbose
    teardown_server
  end

  def test_jruby_4826
    assert_nothing_raised do
      100.times do
        http = Net::HTTP.new('localhost', PORT)
        http.use_ssl = true
        http.verify_mode = OpenSSL::SSL::VERIFY_NONE
        req = Net::HTTP::Post.new('/post')
        http.request(req).body
      end
    end
  end

private

  def make_certificate(key, cn)
    subject = OpenSSL::X509::Name.parse("/DC=org/DC=ruby-lang/CN=#{cn}")
    exts = [
      ["keyUsage", "keyEncipherment,digitalSignature", true],
    ]
    OpenSSL::TestUtils.issue_cert(
      subject, key, 1, Time.now, Time.now + 3600, exts,
      nil, nil, OpenSSL::Digest::SHA1.new
    )
  end

  def setup_server
    key = OpenSSL::TestUtils::TEST_KEY_RSA1024
    cert = make_certificate(key, "localhost")
    logger = Logger.new(STDERR)
    logger.level = Logger::Severity::FATAL	# avoid logging SSLError (ERROR level)
    @server = WEBrick::HTTPServer.new(
      :Logger => logger,
      :Port => PORT,
      :AccessLog => [],
      :SSLEnable => true,
      :ServerName => "localhost",
      :SSLCertificate => cert,
      :SSLPrivateKey => key
    )
    @server.mount(
      "/post",
      WEBrick::HTTPServlet::ProcHandler.new(method("do_post").to_proc)
    )
    @server_thread = start_server_thread(@server)
  end

  def do_post(req, res)
    res.chunked = true
    res['content-type'] = 'text/plain'
    piper, pipew = IO.pipe
    res.body = piper
    10.times { pipew << "A" * 10 }
    pipew.close
  end

  def start_server_thread(server)
    t = Thread.new {
      Thread.current.abort_on_exception = true
      server.start
    }
    while server.status != :Running
      Thread.pass
      unless t.alive?
	t.join
	raise
      end
    end
    t
  end

  def teardown_server
    @server.shutdown if @server
  end
end
