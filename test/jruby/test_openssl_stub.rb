require 'test/unit'
require 'openssl'

class TestOpensslStub < Test::Unit::TestCase
  def test_autoload_consts_error
    # This test only makes sense if the gem isn't installed
    if defined? OpenSSL::OPENSSL_DUMMY
      assert_raise(LoadError) { OpenSSL::ASN1 }
      assert_raise(LoadError) { OpenSSL::BN }
      assert_raise(LoadError) { OpenSSL::Cipher }
      assert_raise(LoadError) { OpenSSL::Config }
      assert_raise(LoadError) { OpenSSL::Netscape }
      assert_raise(LoadError) { OpenSSL::PKCS7 }
      assert_raise(LoadError) { OpenSSL::PKey }
      assert_raise(LoadError) { OpenSSL::Random }
      assert_raise(LoadError) { OpenSSL::SSL }
      assert_raise(LoadError) { OpenSSL::X509 }
    end
  end
end
