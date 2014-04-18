require_relative 'utils'

class OpenSSL::TestHMAC < Test::Unit::TestCase
  def setup
    @digest = OpenSSL::Digest::MD5
    @key = "KEY"
    @data = "DATA"
    @h1 = OpenSSL::HMAC.new(@key, @digest.new)
    @h2 = OpenSSL::HMAC.new(@key, "MD5")
  end

  def teardown
  end

  def test_hmac
    @h1.update(@data)
    @h2.update(@data)
    assert_equal(@h1.digest, @h2.digest)

    assert_equal(OpenSSL::HMAC.digest(@digest.new, @key, @data), @h1.digest, "digest")
    assert_equal(OpenSSL::HMAC.hexdigest(@digest.new, @key, @data), @h1.hexdigest, "hexdigest")

    assert_equal(OpenSSL::HMAC.digest("MD5", @key, @data), @h2.digest, "digest")
    assert_equal(OpenSSL::HMAC.hexdigest("MD5", @key, @data), @h2.hexdigest, "hexdigest")
  end

  def test_dup
    @h1.update(@data)
    h = @h1.dup
    assert_equal(@h1.digest, h.digest, "dup digest")
  end

  def test_to_s
    @h1.update(''); @h1.update('1234567890')
    assert_equal(@h1.hexdigest, @h1.to_s)
    assert_equal(@h2.hexdigest, @h2.to_s)
  end

  def test_reset
    data = 'He is my neighbor Nursultan Tuliagby. He is pain in my assholes.'
    @h1.update('4'); @h1.update('2')
    @h1.reset
    @h1.update(data)
    @h2.update(data)
    assert_equal(@h2.digest, @h1.digest)
  end

  def test_correct_digest
    assert_equal('c17c7b655b11574fea8d676a1fdc0ca8', @h2.hexdigest) # calculated on MRI
    @h2.update('DATA')
    assert_equal('9e50596c0fa1197f8587443a942d8afc', @h2.hexdigest) # calculated on MRI
  end

end if defined?(OpenSSL)
