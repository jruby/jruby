begin
  require "openssl"
rescue LoadError
end

require "test/unit"
require "tempfile"

class TestX509Store < Test::Unit::TestCase
  def setup
    @store = OpenSSL::X509::Store.new
  end

  def path(file)
    File.expand_path(file, File.dirname(__FILE__))
  end

  def teardown
  end

  def test_ns_cert_type
    f = Tempfile.new("globalsign-root.pem")
    f << GLOBALSIGN_ROOT_CA
    f.close
    @store.add_file(f.path)
    f.unlink

    # CAUTION !
    #
    # sgc is an issuing CA certificate so we should not verify it for the
    # purpose 'PURPOSE_SSL_SERVER'. It's not a SSL server certificate.
    # We're just checking the code for 'PURPOSE_SSL_SERVER'.
    # jruby-openssl/0.5.2 raises the following exception around ASN.1
    # nsCertType handling.
    #   Purpose.java:344:in `call': java.lang.ClassCastException: org.bouncycastle.asn1.DEROctetString cannot be cast to org.bouncycastle.asn1.DERBitString
    sgc = OpenSSL::X509::Certificate.new(GLOBALSIGN_ORGANIZATION_VALIDATION_CA)

    @store.purpose = OpenSSL::X509::PURPOSE_SSL_SERVER
    assert_nothing_raised do
      @store.verify(sgc) # => should be false
    end
  end

  def test_purpose_ssl_client
    @store.add_file(path("fixture/purpose/cacert.pem"))
    cert = OpenSSL::X509::Certificate.new(File.read(path("fixture/purpose/sslclient.pem")))
    @store.purpose = OpenSSL::X509::PURPOSE_SSL_CLIENT
    assert_equal(true, @store.verify(cert))
    @store.purpose = OpenSSL::X509::PURPOSE_SSL_SERVER
    assert_equal(false, @store.verify(cert))
    @store.purpose = OpenSSL::X509::PURPOSE_SSL_CLIENT
    assert_equal(true, @store.verify(cert))
  end

  def test_purpose_ssl_server
    @store.add_file(path("fixture/purpose/cacert.pem"))
    cert = OpenSSL::X509::Certificate.new(File.read(path("fixture/purpose/sslserver.pem")))
    @store.purpose = OpenSSL::X509::PURPOSE_SSL_SERVER
    assert_equal(true, @store.verify(cert))
    @store.purpose = OpenSSL::X509::PURPOSE_SSL_CLIENT
    assert_equal(false, @store.verify(cert))
    @store.purpose = OpenSSL::X509::PURPOSE_SSL_SERVER
    assert_equal(true, @store.verify(cert))
  end

  # keyUsage: no digitalSignature bit, keyEncipherment bit only.
  def test_purpose_ssl_server_no_dsig_in_keyUsage
    @store.add_file(path("fixture/purpose/cacert.pem"))
    cert = OpenSSL::X509::Certificate.new(File.read(path("fixture/purpose/sslserver_no_dsig_in_keyUsage.pem")))
    @store.purpose = OpenSSL::X509::PURPOSE_SSL_SERVER
    assert_equal(true, @store.verify(cert))
  end

  def test_add_file_multiple
    f = Tempfile.new("globalsign-root.pem")
    f << GLOBALSIGN_ROOT_CA
    f << "junk junk\n"
    f << "junk junk\n"
    f << "junk junk\n"
    f << File.read(path("fixture/purpose/cacert.pem"))
    f.close
    @store.add_file(f.path)
    f.unlink

    cert = OpenSSL::X509::Certificate.new(File.read(path("fixture/purpose/sslserver.pem")))
    @store.purpose = OpenSSL::X509::PURPOSE_SSL_SERVER
    assert_equal(true, @store.verify(cert))
    @store.purpose = OpenSSL::X509::PURPOSE_SSL_CLIENT
    assert_equal(false, @store.verify(cert))
    @store.purpose = OpenSSL::X509::PURPOSE_SSL_SERVER
    assert_equal(true, @store.verify(cert))
  end

  # jruby-openssl/0.6 raises "can't store certificate" because of duplicated
  # subject. ruby-openssl just ignores the second certificate.
  def test_add_file_JRUBY_4409
    assert_nothing_raised do
      @store.add_file(path("fixture/ca-bundle.crt"))
    end
  end

  def test_set_default_paths
    @store.purpose = OpenSSL::X509::PURPOSE_SSL_SERVER
    cert = OpenSSL::X509::Certificate.new(File.read(path("fixture/purpose/sslserver.pem")))
    assert_equal(false, @store.verify(cert))
    begin
      backup = ENV['SSL_CERT_DIR']
      ENV['SSL_CERT_DIR'] = path('fixture/purpose/')
      @store.set_default_paths
      assert_equal(true, @store.verify(cert))
    ensure
      ENV['SSL_CERT_DIR'] = backup if backup
    end
  end

  GLOBALSIGN_ROOT_CA = <<__EOS__
-----BEGIN CERTIFICATE-----
MIIDdTCCAl2gAwIBAgILBAAAAAABFUtaw5QwDQYJKoZIhvcNAQEFBQAwVzELMAkG
A1UEBhMCQkUxGTAXBgNVBAoTEEdsb2JhbFNpZ24gbnYtc2ExEDAOBgNVBAsTB1Jv
b3QgQ0ExGzAZBgNVBAMTEkdsb2JhbFNpZ24gUm9vdCBDQTAeFw05ODA5MDExMjAw
MDBaFw0yODAxMjgxMjAwMDBaMFcxCzAJBgNVBAYTAkJFMRkwFwYDVQQKExBHbG9i
YWxTaWduIG52LXNhMRAwDgYDVQQLEwdSb290IENBMRswGQYDVQQDExJHbG9iYWxT
aWduIFJvb3QgQ0EwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDaDuaZ
jc6j40+Kfvvxi4Mla+pIH/EqsLmVEQS98GPR4mdmzxzdzxtIK+6NiY6arymAZavp
xy0Sy6scTHAHoT0KMM0VjU/43dSMUBUc71DuxC73/OlS8pF94G3VNTCOXkNz8kHp
1Wrjsok6Vjk4bwY8iGlbKk3Fp1S4bInMm/k8yuX9ifUSPJJ4ltbcdG6TRGHRjcdG
snUOhugZitVtbNV4FpWi6cgKOOvyJBNPc1STE4U6G7weNLWLBYy5d4ux2x8gkasJ
U26Qzns3dLlwR5EiUWMWea6xrkEmCMgZK9FGqkjWZCrXgzT/LCrBbBlDSgeF59N8
9iFo7+ryUp9/k5DPAgMBAAGjQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNVHRMBAf8E
BTADAQH/MB0GA1UdDgQWBBRge2YaRQ2XyolQL30EzTSo//z9SzANBgkqhkiG9w0B
AQUFAAOCAQEA1nPnfE920I2/7LqivjTFKDK1fPxsnCwrvQmeU79rXqoRSLblCKOz
yj1hTdNGCbM+w6DjY1Ub8rrvrTnhQ7k4o+YviiY776BQVvnGCv04zcQLcFGUl5gE
38NflNUVyRRBnMRddWQVDf9VMOyGj/8N7yy5Y0b2qvzfvGn9LhJIZJrglfCm7ymP
AbEVtQwdpf5pLGkkeB6zpxxxYu7KyJesF12KwvhHhm4qxFYxldBniYUr+WymXUad
DKqC5JlR3XC321Y9YeRq4VzW9v493kHMB65jUr9TU/Qr6cf9tveCX4XSQRjbgbME
HMUfpIBvFSDJ3gyICh3WZlXi/EjJKSZp4A==
-----END CERTIFICATE-----
__EOS__

  GLOBALSIGN_ORGANIZATION_VALIDATION_CA = <<__EOS__
-----BEGIN CERTIFICATE-----
MIIEZzCCA0+gAwIBAgILBAAAAAABHkSl9SowDQYJKoZIhvcNAQEFBQAwVzELMAkG
A1UEBhMCQkUxGTAXBgNVBAoTEEdsb2JhbFNpZ24gbnYtc2ExEDAOBgNVBAsTB1Jv
b3QgQ0ExGzAZBgNVBAMTEkdsb2JhbFNpZ24gUm9vdCBDQTAeFw0wNzA0MTExMjAw
MDBaFw0xNzA0MTExMjAwMDBaMGoxIzAhBgNVBAsTGk9yZ2FuaXphdGlvbiBWYWxp
ZGF0aW9uIENBMRMwEQYDVQQKEwpHbG9iYWxTaWduMS4wLAYDVQQDEyVHbG9iYWxT
aWduIE9yZ2FuaXphdGlvbiBWYWxpZGF0aW9uIENBMIIBIjANBgkqhkiG9w0BAQEF
AAOCAQ8AMIIBCgKCAQEAoS/EvM6HA+lnwYnI5ZP8fbStnvZjTmronCxziaIB9I8h
+P0lnVgWbYb27klXdX516iIRfj37x0JB3PzFDJFVgHvrZDMdm/nKOOmrxiVDUSVA
9OR+GFVqqY8QOkAe1leD738vNC8t0vZTwhkNt+3JgfVGLLQjQl6dEwN17Opq/Fd8
yTaXO5jcExPs7EH6XTTquZPnEBZlzJyS/fXFnT5KuQn85F8eaV9N9FZyRLEdIwPI
NvZliMi/ORZFjh4mbFEWxSoAOMWkE2mVfasBO6jEFLSA2qwaRCDV/qkGexQnr+Aw
Id2Q9KnVIxkuHgPmwd+VKeTBlEPdPpCqy0vJvorTOQIDAQABo4IBHzCCARswDgYD
VR0PAQH/BAQDAgEGMBIGA1UdEwEB/wQIMAYBAf8CAQAwHQYDVR0OBBYEFH1tKuxm
q6dRNqsCafFwj8RZC5ofMEsGA1UdIAREMEIwQAYJKwYBBAGgMgEUMDMwMQYIKwYB
BQUHAgEWJWh0dHA6Ly93d3cuZ2xvYmFsc2lnbi5uZXQvcmVwb3NpdG9yeS8wMwYD
VR0fBCwwKjAooCagJIYiaHR0cDovL2NybC5nbG9iYWxzaWduLm5ldC9yb290LmNy
bDARBglghkgBhvhCAQEEBAMCAgQwIAYDVR0lBBkwFwYKKwYBBAGCNwoDAwYJYIZI
AYb4QgQBMB8GA1UdIwQYMBaAFGB7ZhpFDZfKiVAvfQTNNKj//P1LMA0GCSqGSIb3
DQEBBQUAA4IBAQB5R/wV10x53w96ns7UfEtjyYm1ez+ZEuicjJpJL+BOlUrtx7y+
8aLbjpMdunFUqkvZiSIkh8UEqKyCUqBS+LjhT6EnZmMhSjnnx8VOX7LWHRNtMOnO
16IcvCkKczxbI0n+1v/KsE/18meYwEcR+LdIppAJ1kK+6rG5U0LDnCDJ+6FbtVZt
h4HIYKzEuXInCo4eqLEuzTKieFewnPiVu0OOjDGGblMNxhIFukFuqDUwCRgdAmH/
/e413mrDO9BNS05QslY2DERd2hplKuaYVqljMy4E567o9I63stp9wMjirqYoL+PJ
c738B0E0t6pu7qfb0ZM87ZDsMpKI2cgjbHQh
-----END CERTIFICATE-----
__EOS__
end
