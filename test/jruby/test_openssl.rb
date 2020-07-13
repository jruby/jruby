require 'test/unit'
require 'openssl'

class TestOpenSSL < Test::Unit::TestCase

  def test_csr_request_extensions
    key = OpenSSL::PKey::RSA.new(512)
    csr = OpenSSL::X509::Request.new

    csr.version = 0
    csr.subject = OpenSSL::X509::Name.new([["CN", 'example.com']])
    csr.public_key = key.public_key

    names = OpenSSL::X509::ExtensionFactory.new.
      create_extension("subjectAltName", 'DNS:example.com', false)

    extReq = OpenSSL::ASN1::Set([OpenSSL::ASN1::Sequence([names])])
    csr.add_attribute(OpenSSL::X509::Attribute.new("extReq", extReq))

    csr.sign(key, OpenSSL::Digest::SHA256.new)

    # The combination of the extreq and the stringification / revivification
    # is what triggers the bad behaviour in the extension. (Any extended
    # request type should do, but this matches my observed problems)
    csr = OpenSSL::X509::Request.new(csr.to_s)

    assert_equal '/CN=example.com', csr.subject.to_s
  end

  if OpenSSL::OPENSSL_VERSION_NUMBER > 0x00908000

    def test_098_features
      sha224_a = "abd37534c7d9a2efb9465de931cd7055ffdb8879563ae98078d6d6d5"
      sha256_a = "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb"
      sha384_a = "54a59b9f22b0b80880d8427e548b7c23abd873486e1f035dce9cd697e85175033caa88e6d57bc35efae0b5afd3145f31"
      sha512_a = "1f40fc92da241694750979ee6cf582f2d5d7d28e18335de05abc54d0560e0f5302860c652bf08d560252aa5e74210546f369fbbbce8c12cfc7957b2652fe9a75"

      assert_equal(sha224_a, OpenSSL::Digest::SHA224.hexdigest("a"))
      assert_equal(sha256_a, OpenSSL::Digest::SHA256.hexdigest("a"))
      assert_equal(sha384_a, OpenSSL::Digest::SHA384.hexdigest("a"))
      assert_equal(sha512_a, OpenSSL::Digest::SHA512.hexdigest("a"))

      assert_equal(sha224_a, encode16(OpenSSL::Digest::SHA224.digest("a")))
      assert_equal(sha256_a, encode16(OpenSSL::Digest::SHA256.digest("a")))
      assert_equal(sha384_a, encode16(OpenSSL::Digest::SHA384.digest("a")))
      assert_equal(sha512_a, encode16(OpenSSL::Digest::SHA512.digest("a")))
    end

    def encode16(str)
      str.unpack("H*").first
    end

  end

end
