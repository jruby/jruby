require 'openssl'
require "test/unit"

class TestPkcs7 < Test::Unit::TestCase

  CERT_PEM = <<END
-----BEGIN CERTIFICATE-----
MIIC8zCCAdugAwIBAgIBATANBgkqhkiG9w0BAQQFADA9MRMwEQYKCZImiZPyLGQB
GRYDb3JnMRkwFwYKCZImiZPyLGQBGRYJcnVieS1sYW5nMQswCQYDVQQDDAJDQTAe
Fw0wOTA1MjMxNTAzNDNaFw0wOTA1MjMxNjAzNDNaMD0xEzARBgoJkiaJk/IsZAEZ
FgNvcmcxGTAXBgoJkiaJk/IsZAEZFglydWJ5LWxhbmcxCzAJBgNVBAMMAkNBMIIB
IjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuV9ht9J7k4NBs38jOXvvTKY9
gW8nLICSno5EETR1cuF7i4pNs9I1QJGAFAX0BEO4KbzXmuOvfCpD3CU+Slp1enen
fzq/t/e/1IRW0wkJUJUFQign4CtrkJL+P07yx18UjyPlBXb81ApEmAB5mrJVSrWm
qbjs07JbuS4QQGGXLc+Su96DkYKmSNVjBiLxVVSpyZfAY3hD37d60uG+X8xdW5v6
8JkRFIhdGlb6JL8fllf/A/blNwdJOhVr9mESHhwGjwfSeTDPfd8ZLE027E5lyAVX
9KZYcU00mOX+fdxOSnGqS/8JDRh0EPHDL15RcJjV2J6vZjPb0rOYGDoMcH+94wID
AQABMA0GCSqGSIb3DQEBBAUAA4IBAQB8UTw1agA9wdXxHMUACduYu6oNL7pdF0dr
w7a4QPJyj62h4+Umxvp13q0PBw0E+mSjhXMcqUhDLjrmMcvvNGhuh5Sdjbe3GI/M
3lCC9OwYYIzzul7omvGC3JEIGfzzdNnPPCPKEWp5X9f0MKLMR79qOf+sjHTjN2BY
SY3YGsEFxyTXDdqrlaYaOtTAdi/C+g1WxR8fkPLefymVwIFwvyc9/bnp7iBn7Hcw
mbxtLPbtQ9mURT0GHewZRTGJ1aiTq9Ag3xXME2FPF04eFRd3mclOQZNXKQ+LDxYf
k0X5FeZvsWf4srFxoVxlcDdJtHh91ZRpDDJYGQlsUm9CPTnO+e4E
-----END CERTIFICATE-----
END

  def test_pkcs7_des3_key_generation_for_encrypt
    # SunJCE requires DES/DES3 keybits = 21/168 for key generation.
    # BC allows 24/192 keybits and treats it as 21/168.
    msg = "Hello World"
    password = "password"
    cert = OpenSSL::X509::Certificate.new(CERT_PEM)
    certs = [cert]
    cipher = OpenSSL::Cipher.new("des-ede3-cbc")
    cipher.encrypt
    cipher.pkcs5_keyivgen(password)
    p7 = OpenSSL::PKCS7.encrypt(certs, msg, cipher, OpenSSL::PKCS7::BINARY)
    assert_equal(msg, p7.data)
  end

  EMPTY_PEM = <<END
-----BEGIN PKCS7-----
MAMGAQA=
-----END PKCS7-----
END

  def test_empty_pkcs7
    p7 = OpenSSL::PKCS7.new
    assert_equal(EMPTY_PEM, p7.to_pem)
  end

  def test_load_empty_pkcs7
    p7 = OpenSSL::PKCS7.new(EMPTY_PEM)
    assert_equal(EMPTY_PEM, p7.to_pem)
  end
end
