require 'test/unit'
require 'openssl'
require 'socket'

class TestOpenssl < Test::Unit::TestCase
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

  def test_ssl_read_with_select # JRUBY-6874
    host = "127.0.0.1"
    port = rand(10000) + 10000
    
    ssl_key = "-----BEGIN RSA PRIVATE KEY-----\nMIICXQIBAAKBgQCvF80yn6D+kqGwMSQHcpHUwCRt+c39Qoy99fCWdenPthfUscec\ny62Ij8+rKYCnoE9y766a5baowdDKqq3IBOZn2Ove3zfueGbHAbWehFopG2xySf0U\nPjdmWk+DRDlCeFLig6xfAnOKWo+N0MViso3dNK8gYzb6FWqlWgZgAcMpswIDAQAB\nAoGAHv/UyZivdULas4oPue3T2dnm2T239ZXZuywW21ym96pij7ql/6Gj6KClgMVJ\nTOQ6DLxYqn3vF/OwlqEfQWF0tTUYY+xNbEDE1YsbrS5/FSzbaEYYOHzRl/vMmnsf\naNgYaSjOIecin7L71Wzq0piMIxg8BLb6IVECBku9EQNzxuECQQDZsbRgg1XZGj+r\nXAu/qXTNKQ/r7k+iPN5bXON6ApBomG+4Q7VVITL3tkGzLOphRZ37Q28FrN4B4gtC\nXb9il5lDAkEAzecTSopPi2VdcME4WWmwn1rbTp/jJNt4dGZLsNfj9RejVDd32i/L\nP7wCpoPDaaVcoF2HgvCs39qatyVg6ecu0QJBALN4q+q9nDMGTuNpWU5D2EWjyrqJ\nmCF66R6NcASQxJlWwxQ4zfBHFIvgOD4Nk5VqHZqet5MIN2d6AipOu4/+x50CQHDp\njf+rd1GHBcXGf8MwnUXWCjvEnEhi/lw+mLVivsRx8QRG4rfIy9monX949Flj8DaU\n87IPj422kG9s1QeP2nECQQCkg+RUcoQm7SiM8OXuXNeHQlvQNp65geFRxzKAXxT/\n+1Mbtwnd3AXXZBekFDDpE9U3ZQjahoe7oc1oUBuw5hXL\n-----END RSA PRIVATE KEY-----\n"
    ssl_cert = "-----BEGIN CERTIFICATE-----\nMIIC/jCCAeagAwIBAgIBAjANBgkqhkiG9w0BAQUFADA5MQswCQYDVQQGEwJVUzEO\nMAwGA1UECgwFbG9jYWwxDTALBgNVBAsMBGFlcm8xCzAJBgNVBAMMAkNBMB4XDTEy\nMDExNDAwMjcyN1oXDTEzMDExMzAwMjcyN1owSDELMAkGA1UEBhMCVVMxDjAMBgNV\nBAoMBWxvY2FsMQ0wCwYDVQQLDARhZXJvMQswCQYDVQQLDAJDQTENMAsGA1UEAwwE\ncHVtYTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEArxfNMp+g/pKhsDEkB3KR\n1MAkbfnN/UKMvfXwlnXpz7YX1LHHnMutiI/PqymAp6BPcu+umuW2qMHQyqqtyATm\nZ9jr3t837nhmxwG1noRaKRtsckn9FD43ZlpPg0Q5QnhS4oOsXwJzilqPjdDFYrKN\n3TSvIGM2+hVqpVoGYAHDKbMCAwEAAaOBhTCBgjAMBgNVHRMBAf8EAjAAMDEGCWCG\nSAGG+EIBDQQkFiJSdWJ5L09wZW5TU0wgR2VuZXJhdGVkIENlcnRpZmljYXRlMB0G\nA1UdDgQWBBTyDyJlmYBDwfWdRj6lWGvoY43k9DALBgNVHQ8EBAMCBaAwEwYDVR0l\nBAwwCgYIKwYBBQUHAwEwDQYJKoZIhvcNAQEFBQADggEBAIbBVfoVCG8RyVesPW+q\n5i0wAMbHZ1fwv1RKp17c68DYDs0YYPi0bA0ss8AgpU6thWmskxPiFaE6D5x8iv9f\nzkcHxgr1Mrbx6RLx9tLUVehSmRv3aiVO4k9Mp6vf+rJK1AYeaGBmvoqTBLwy7Jrt\nytKMdqMJj5jKWkWgEGgTnjzbcOClmCQab9isigIzTxMyC/LjeKZe8pPeVX6OM8bY\ny8XGZp9B7uwdPzqt/g25IzTC0KsQwq8cB0raAtZzIyTNv42zcUjmQNVazAozCTcq\nMsEtK2z7TYBC3udTsdyS2qVqCpsk7IMOBGrw8vk4SNhO+coiDObW2K/HNvhl0tZC\noQI=\n-----END CERTIFICATE-----"
    
    ctx = OpenSSL::SSL::SSLContext.new
    ctx.key = OpenSSL::PKey::RSA.new ssl_key
    ctx.cert = OpenSSL::X509::Certificate.new ssl_cert
    ctx.verify_mode = OpenSSL::SSL::VERIFY_NONE
    
    tcp_server = TCPServer.new(host, port)
    @server = OpenSSL::SSL::SSLServer.new(tcp_server, ctx)
    
    num_ary = []
    t = Thread.new do
      ios = IO.select [@server]
      ios.first.each do |sock|
        c = sock.accept
        20.times do |i|
          ready = IO.select [c]
          ready.first.each do |c|
            num_ary << c.read(1) # read and readpartial are both broken for this test case
          end
        end
      end
    end
    
    client = TCPSocket.new host, port
    sslclient = OpenSSL::SSL::SSLSocket.new(client)
    sslclient.connect
    sslclient.write "01234567890123456789"
    
    t.join
    
    assert_equal((0..9).to_a.map(&:to_s) * 2, num_ary)
  end
end
