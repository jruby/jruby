require 'test/unit'
require 'openssl'

class TestOpenssl < Test::Unit::TestCase
  def test_adding_pem
OpenSSL.debug = true
    # mimic what rubygems/request#add_rubygems_trusted_certs does
    # to find the pem certificates
    base = $LOAD_PATH.detect { |p| p =~ /shared/ }
    pems = Dir[ File.join(base, 'rubygems/ssl_certs/*pem') ]
    assert_equal( 9, pems.size )
    pems.each do |pem|
      store = OpenSSL::X509::Store.new
      cert = OpenSSL::X509::Certificate.new(File.read(pem))
      assert( !store.verify(cert) )
      store.add_file(pem)
      # only verify on self signed certifactes
      assert( store.verify(cert) ) unless pem =~ /AddTrustExternalCARoot.pem/
    end
  end
end
