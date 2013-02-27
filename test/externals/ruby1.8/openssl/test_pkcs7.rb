begin
  require "openssl"
  require File.join(File.dirname(__FILE__), "utils.rb")
rescue LoadError
end
require "test/unit"

if defined?(OpenSSL)

class OpenSSL::TestPKCS7 < Test::Unit::TestCase
  def setup
    @rsa1024 = OpenSSL::TestUtils::TEST_KEY_RSA1024
    @rsa2048 = OpenSSL::TestUtils::TEST_KEY_RSA2048
    ca = OpenSSL::X509::Name.parse("/DC=org/DC=ruby-lang/CN=CA")
    ee1 = OpenSSL::X509::Name.parse("/DC=org/DC=ruby-lang/CN=EE1")
    ee2 = OpenSSL::X509::Name.parse("/DC=org/DC=ruby-lang/CN=EE2")

    now = Time.now
    ca_exts = [
      ["basicConstraints","CA:TRUE",true],
      ["keyUsage","keyCertSign, cRLSign",true],
      ["subjectKeyIdentifier","hash",false],
      ["authorityKeyIdentifier","keyid:always",false],
    ]
    @ca_cert = issue_cert(ca, @rsa2048, 1, Time.now, Time.now+3600, ca_exts,
                           nil, nil, OpenSSL::Digest::SHA1.new)
    ee_exts = [
      ["keyUsage","Non Repudiation, Digital Signature, Key Encipherment",true],
      ["authorityKeyIdentifier","keyid:always",false],
      ["extendedKeyUsage","clientAuth, emailProtection, codeSigning",false],
      ["nsCertType","client,email",false],
    ]
    @ee1_cert = issue_cert(ee1, @rsa1024, 2, Time.now, Time.now+1800, ee_exts,
                           @ca_cert, @rsa2048, OpenSSL::Digest::SHA1.new)
    @ee2_cert = issue_cert(ee2, @rsa1024, 3, Time.now, Time.now+1800, ee_exts,
                           @ca_cert, @rsa2048, OpenSSL::Digest::SHA1.new)
  end

  def issue_cert(*args)
    OpenSSL::TestUtils.issue_cert(*args)
  end

  def test_signed
    store = OpenSSL::X509::Store.new
    store.add_cert(@ca_cert)
    ca_certs = [@ca_cert]

    data = "aaaaa\r\nbbbbb\r\nccccc\r\n"
    tmp = OpenSSL::PKCS7.sign(@ee1_cert, @rsa1024, data, ca_certs)
    p7 = OpenSSL::PKCS7.new(tmp.to_der)
    certs = p7.certificates
    signers = p7.signers
    assert(p7.verify([], store))
    assert_equal(data, p7.data)
    assert_equal(2, certs.size)
    assert_equal(@ee1_cert.subject.to_s, certs[0].subject.to_s)
    assert_equal(@ca_cert.subject.to_s, certs[1].subject.to_s)
    assert_equal(1, signers.size)
    assert_equal(@ee1_cert.serial, signers[0].serial)
    assert_equal(@ee1_cert.issuer.to_s, signers[0].issuer.to_s)

    # Normaly OpenSSL tries to translate the supplied content into canonical
    # MIME format (e.g. a newline character is converted into CR+LF).
    # If the content is a binary, PKCS7::BINARY flag should be used.

    data = "aaaaa\nbbbbb\nccccc\n"
    flag = OpenSSL::PKCS7::BINARY
    tmp = OpenSSL::PKCS7.sign(@ee1_cert, @rsa1024, data, ca_certs, flag)
    p7 = OpenSSL::PKCS7.new(tmp.to_der)
    certs = p7.certificates
    signers = p7.signers
    assert(p7.verify([], store))
    assert_equal(data, p7.data)
    assert_equal(2, certs.size)
    assert_equal(@ee1_cert.subject.to_s, certs[0].subject.to_s)
    assert_equal(@ca_cert.subject.to_s, certs[1].subject.to_s)
    assert_equal(1, signers.size)
    assert_equal(@ee1_cert.serial, signers[0].serial)
    assert_equal(@ee1_cert.issuer.to_s, signers[0].issuer.to_s)

    # A signed-data which have multiple signatures can be created
    # through the following steps.
    #   1. create two signed-data
    #   2. copy signerInfo and certificate from one to another

    tmp1 = OpenSSL::PKCS7.sign(@ee1_cert, @rsa1024, data, [], flag)
    tmp2 = OpenSSL::PKCS7.sign(@ee2_cert, @rsa1024, data, [], flag)
    tmp1.add_signer(tmp2.signers[0])
    tmp1.add_certificate(@ee2_cert)

    p7 = OpenSSL::PKCS7.new(tmp1.to_der)
    certs = p7.certificates
    signers = p7.signers
    assert(p7.verify([], store))
    assert_equal(data, p7.data)
    assert_equal(2, certs.size)
    assert_equal(2, signers.size)
    assert_equal(@ee1_cert.serial, signers[0].serial)
    assert_equal(@ee1_cert.issuer.to_s, signers[0].issuer.to_s)
    assert_equal(@ee2_cert.serial, signers[1].serial)
    assert_equal(@ee2_cert.issuer.to_s, signers[1].issuer.to_s)
  end

  def test_detached_sign
    store = OpenSSL::X509::Store.new
    store.add_cert(@ca_cert)
    ca_certs = [@ca_cert]

    data = "aaaaa\nbbbbb\nccccc\n"
    flag = OpenSSL::PKCS7::BINARY|OpenSSL::PKCS7::DETACHED
    tmp = OpenSSL::PKCS7.sign(@ee1_cert, @rsa1024, data, ca_certs, flag)
    p7 = OpenSSL::PKCS7.new(tmp.to_der)
    a1 = OpenSSL::ASN1.decode(p7)

    certs = p7.certificates
    signers = p7.signers
    assert(!p7.verify([], store))
    assert(p7.verify([], store, data))
    assert_equal(data, p7.data)
    assert_equal(2, certs.size)
    assert_equal(@ee1_cert.subject.to_s, certs[0].subject.to_s)
    assert_equal(@ca_cert.subject.to_s, certs[1].subject.to_s)
    assert_equal(1, signers.size)
    assert_equal(@ee1_cert.serial, signers[0].serial)
    assert_equal(@ee1_cert.issuer.to_s, signers[0].issuer.to_s)
  end

  def test_enveloped
    if OpenSSL::OPENSSL_VERSION_NUMBER <= 0x0090704f
      # PKCS7_encrypt() of OpenSSL-0.9.7d goes to SEGV.
      # http://www.mail-archive.com/openssl-dev@openssl.org/msg17376.html
      return
    end

    certs = [@ee1_cert, @ee2_cert]
    cipher = OpenSSL::Cipher::AES.new("128-CBC")
    data = "aaaaa\nbbbbb\nccccc\n"

    tmp = OpenSSL::PKCS7.encrypt(certs, data, cipher, OpenSSL::PKCS7::BINARY)
    p7 = OpenSSL::PKCS7.new(tmp.to_der)
    recip = p7.recipients
    assert_equal(:enveloped, p7.type)
    assert_equal(2, recip.size)

    assert_equal(@ca_cert.subject.to_s, recip[0].issuer.to_s)
    assert_equal(2, recip[0].serial)
    assert_equal(data, p7.decrypt(@rsa1024, @ee1_cert))

    assert_equal(@ca_cert.subject.to_s, recip[1].issuer.to_s)
    assert_equal(3, recip[1].serial)
    assert_equal(data, p7.decrypt(@rsa1024, @ee2_cert))
  end

  def test_envelope_des3
    certs = [@ee1_cert]
    cipher = OpenSSL::Cipher.new("des-ede3-cbc")
    data = "aaaaa\nbbbbb\nccccc\n"
    tmp = OpenSSL::PKCS7.encrypt(certs, data, cipher, OpenSSL::PKCS7::BINARY)
    p7 = OpenSSL::PKCS7.new(tmp.to_der)
    assert_equal(data, p7.decrypt(@rsa1024, @ee1_cert))
  end

  def test_envelope_nil # RC2-40-CBC by default
    certs = [@ee1_cert]
    data = "aaaaa\nbbbbb\nccccc\n"
    tmp = OpenSSL::PKCS7.encrypt(certs, data, nil, OpenSSL::PKCS7::BINARY)
    p7 = OpenSSL::PKCS7.new(tmp.to_der)
    assert_equal(data, p7.decrypt(@rsa1024, @ee1_cert))
  end

  def test_envelope_des3_compat
    data = "aaaaa\nbbbbb\nccccc\n"
    cruby_envelope = <<EOP
-----BEGIN PKCS7-----
MIIBMgYJKoZIhvcNAQcDoIIBIzCCAR8CAQAxgdwwgdkCAQAwQjA9MRMwEQYKCZIm
iZPyLGQBGRYDb3JnMRkwFwYKCZImiZPyLGQBGRYJcnVieS1sYW5nMQswCQYDVQQD
DAJDQQIBAjANBgkqhkiG9w0BAQEFAASBgECDOPwRb0Vimo3bXAypvnhB/JvHZ0hV
5CWFdAmovioiu1fnMEqawJWudznUZ1rsCKKX4qzqfvSXk+8w7IZ5rqEFoGmLRQQ+
GR8yPJnDwNyQJwRjvcX2WzJnFDFIfROb+ySu8UCmxkTd/5jB3jsREXVqSIxezTif
IT8Q8X7CCx8+MDsGCSqGSIb3DQEHATAUBggqhkiG9w0DBwQIaH1JJe6+hX+AGD8E
j3/kwFY3IOUxly+lPJNEQLpWBoSHZA==
-----END PKCS7-----
EOP
    p7 = OpenSSL::PKCS7.new(cruby_envelope)
    assert_equal(data, p7.decrypt(@rsa1024, @ee1_cert))
    #
    jruby_envelope = <<EOP
-----BEGIN PKCS7-----
MIIBMAYJKoZIhvcNAQcDoIIBITCCAR0CAQAxgdowgdcCAQAwQjA9MRMwEQYKCZIm
iZPyLGQBGRYDb3JnMRkwFwYKCZImiZPyLGQBGRYJcnVieS1sYW5nMQswCQYDVQQD
DAJDQQIBAjALBgkqhkiG9w0BAQEEgYBqCQY/oP0Gv1XbAJ5HjZ9HNZN9gBFlmMDx
fb9YWDQZH24KrTUEssr6jyJuyMsONTdaYWIfG/RWHxw970AkXUXcXDeO8Ze+vSVh
8tohLGLTsBKdvizuC/5jFHLAoNaa5qJZEFanmqMXlO5HiImUZB2BHwJddRuRTg0y
UuAnFtLd+DA7BgkqhkiG9w0BBwEwFAYIKoZIhvcNAwcECP1rHLNHCtyWgBgFQDex
XDgcukPOkDwRcUQJAKu3x5HtQpw=
-----END PKCS7-----
EOP
    p7 = OpenSSL::PKCS7.new(jruby_envelope)
    assert_equal(data, p7.decrypt(@rsa1024, @ee1_cert))
  end

  def test_envelope_aes_compat
    data = "aaaaa\nbbbbb\nccccc\n"
    cruby_envelope = <<EOP
-----BEGIN PKCS7-----
MIICIAYJKoZIhvcNAQcDoIICETCCAg0CAQAxggG4MIHZAgEAMEIwPTETMBEGCgmS
JomT8ixkARkWA29yZzEZMBcGCgmSJomT8ixkARkWCXJ1YnktbGFuZzELMAkGA1UE
AwwCQ0ECAQIwDQYJKoZIhvcNAQEBBQAEgYCHIMVl+WKzjnTuslePlItMq4A+klIZ
rU+5U0UvaOPPpr2UgjD3J1OL09W19De7pKNSSZUd0QWQBB3IG4IzefWzYxt2ejZY
rJDO/wdHa6Mdq1ZsdbLP1sIRxTyWskc3O8VJvo5boFG/bZxLHA6CPnhifnfqEkkq
wVbjAbBGI61HxTCB2QIBADBCMD0xEzARBgoJkiaJk/IsZAEZFgNvcmcxGTAXBgoJ
kiaJk/IsZAEZFglydWJ5LWxhbmcxCzAJBgNVBAMMAkNBAgEDMA0GCSqGSIb3DQEB
AQUABIGASvO7jsPCAB/TcRgmIKEHRDqPThQrSAJRE+uDVeiPlIHsCaUDspGX8niH
4+UPsLhdd6H68Ecay93Hi78SYR/w0NbrwwMBGRlU3/AFhq/OseosuBb303mAqnoz
kU6qlNwJuy/4NIReldsaVJJuZ4nkEBfZAw+99Mxh7IQYx069fwIwTAYJKoZIhvcN
AQcBMB0GCWCGSAFlAwQBAgQQf1IrOpN2OmqMHz1t7biX/oAgubIiBzarCuTKPMby
eg4/+hy0xJsT0IkF1O0G1XTOWcE=
-----END PKCS7-----
EOP
    p7 = OpenSSL::PKCS7.new(cruby_envelope)
    assert_equal(data, p7.decrypt(@rsa1024, @ee1_cert))
    #
    jruby_envelope = <<EOP
-----BEGIN PKCS7-----
MIICHAYJKoZIhvcNAQcDoIICDTCCAgkCAQAxggG0MIHXAgEAMEIwPTETMBEGCgmS
JomT8ixkARkWA29yZzEZMBcGCgmSJomT8ixkARkWCXJ1YnktbGFuZzELMAkGA1UE
AwwCQ0ECAQIwCwYJKoZIhvcNAQEBBIGAg0Yz54LwCKM9l128jjh0FlA5Wvzfsjd2
S3dYESzxnxqdhKkSDya16lkYyZZ+aVWmC8XOgkGGwGJTudq3gGn2p3wsgx63J4Ar
PfslsDslIaddp8op4i+ifDi15qCjWXIyQaYMSN/DsFN8DlB8jMjPAlQO3MFtifb2
D7vFjLjSrogwgdcCAQAwQjA9MRMwEQYKCZImiZPyLGQBGRYDb3JnMRkwFwYKCZIm
iZPyLGQBGRYJcnVieS1sYW5nMQswCQYDVQQDDAJDQQIBAzALBgkqhkiG9w0BAQEE
gYCfAEL80vCsFo9kalePlb73lL2iDPbbDfjpWs0nnlXX8BhS/H781kvUkDpwl/qT
9KcFCaPGJ2IYgEjys6VPK9ho/hIIIz+BX8MIuWbweQTn1Y0TTlTL91Zr66xyZP1p
zyStG6Zc1u26hiX31hk1P6ihhhXu+I5bserKNYUnYsxJSjBMBgkqhkiG9w0BBwEw
HQYJYIZIAWUDBAECBBD42Hndr47SEdUoc6SWOKsbgCCylxb34kE14eBc9nN9MnC+
SaVrDPgso584FIimP6o+Fw==
-----END PKCS7-----
EOP
    p7 = OpenSSL::PKCS7.new(jruby_envelope)
    assert_equal(data, p7.decrypt(@rsa1024, @ee1_cert))
  end

  def test_signed_compat
=begin
    # how to generate signature
    ca_certs = [@ca_cert]
    data = "aaaaa\r\nbbbbb\r\nccccc\r\n"
    tmp = OpenSSL::PKCS7.sign(@ee1_cert, @rsa1024, data, ca_certs)
    puts tmp
=end
    cruby_sign = <<EOP
-----BEGIN PKCS7-----
MIIILgYJKoZIhvcNAQcCoIIIHzCCCBsCAQExCzAJBgUrDgMCGgUAMCQGCSqGSIb3
DQEHAaAXBBVhYWFhYQ0KYmJiYmINCmNjY2NjDQqgggZBMIIC4TCCAcmgAwIBAgIB
AjANBgkqhkiG9w0BAQUFADA9MRMwEQYKCZImiZPyLGQBGRYDb3JnMRkwFwYKCZIm
iZPyLGQBGRYJcnVieS1sYW5nMQswCQYDVQQDDAJDQTAeFw0wOTEyMTYxNTQ1MzRa
Fw0wOTEyMTYxNjE1MzRaMD4xEzARBgoJkiaJk/IsZAEZFgNvcmcxGTAXBgoJkiaJ
k/IsZAEZFglydWJ5LWxhbmcxDDAKBgNVBAMMA0VFMTCBnzANBgkqhkiG9w0BAQEF
AAOBjQAwgYkCgYEAy8LEsNRApz7U/j5DoB4XBgO9Z8Atv5y/OVQRp0ag8Tqo1Yew
sWijxEWB7JOATwpBN267U4T1nPZIxxEEO7n/WNa2ws9JWsjah8ssEBFSxZqdXKSL
f0N4Hi7/GQ/aYoaMCiQ8jA4jegK2FJmXM71uPe+jFN/peeBOpRfyXxRFOYcCAwEA
AaNvMG0wDgYDVR0PAQH/BAQDAgXgMB8GA1UdIwQYMBaAFJc5ncP7zbqPVAyQe0Y/
6tZDdbHLMCcGA1UdJQQgMB4GCCsGAQUFBwMCBggrBgEFBQcDBAYIKwYBBQUHAwMw
EQYJYIZIAYb4QgEBBAQDAgWgMA0GCSqGSIb3DQEBBQUAA4IBAQB9jL0H9qAeWZmA
lmEr7WbVibFwod6ZgNmbFhoP6a9PANDdYwp1EQ7J2o3Dzw1hNjsxDVE5uf3qgA0F
df/YoFkfi4xoL1pKdZv9ZMOlctC1po7MbFakjeHdxMtdIM70DMxbS4o4HzXrKtC3
of1SmKh+g+r4R1YHCrbBCspEX+s2Y4mKD0IP0XkVvv1d4YICAnKYGCYEC9OS4fr7
JPB2cL1yXnjPL0OOvSeAOC2uIkDq1SVZk6Xq4sSaHAKwBNGg0HrqOhrdgcB0Ftpi
7Paty9PUmSIjoqre/WzfGNF1MrtTC0wf0PDw/aUzWgInlIXJhcbJOMyhWM/SO5ok
50rcYfObMIIDWDCCAkCgAwIBAgIBATANBgkqhkiG9w0BAQUFADA9MRMwEQYKCZIm
iZPyLGQBGRYDb3JnMRkwFwYKCZImiZPyLGQBGRYJcnVieS1sYW5nMQswCQYDVQQD
DAJDQTAeFw0wOTEyMTYxNTQ1MzRaFw0wOTEyMTYxNjQ1MzRaMD0xEzARBgoJkiaJ
k/IsZAEZFgNvcmcxGTAXBgoJkiaJk/IsZAEZFglydWJ5LWxhbmcxCzAJBgNVBAMM
AkNBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuV9ht9J7k4NBs38j
OXvvTKY9gW8nLICSno5EETR1cuF7i4pNs9I1QJGAFAX0BEO4KbzXmuOvfCpD3CU+
Slp1enenfzq/t/e/1IRW0wkJUJUFQign4CtrkJL+P07yx18UjyPlBXb81ApEmAB5
mrJVSrWmqbjs07JbuS4QQGGXLc+Su96DkYKmSNVjBiLxVVSpyZfAY3hD37d60uG+
X8xdW5v68JkRFIhdGlb6JL8fllf/A/blNwdJOhVr9mESHhwGjwfSeTDPfd8ZLE02
7E5lyAVX9KZYcU00mOX+fdxOSnGqS/8JDRh0EPHDL15RcJjV2J6vZjPb0rOYGDoM
cH+94wIDAQABo2MwYTAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjAd
BgNVHQ4EFgQUlzmdw/vNuo9UDJB7Rj/q1kN1scswHwYDVR0jBBgwFoAUlzmdw/vN
uo9UDJB7Rj/q1kN1scswDQYJKoZIhvcNAQEFBQADggEBAFa1X5xX5+NlXOI3z2vh
Vp9tPvIAtftqkhdMbfS1dAAIIZKVLPfvQ+ZLqx/AzQXmDajg3Pg9YoBB3RRDx1xh
A9ECO4Lpbv5fYAkIul6XQ2D3U1IjnkhdfYHcU5iRl58nhjlDNd+3vOp1/h9D9Pp6
lRILuFCoRcOogcXzChuDA06CDbMao1dDcwdNe1SdV54hzZs1DVqoKIjj4182HUST
getU2RDFXh76VtF35iYDzdA+iCAWOqXSMAq7GnZJvL//0Ndffc7Oc6QXCicwiUSw
Wrj72gEakBOeC8XxlYaP7TSXFkasdg1Eccz7+U6LgWaYrgwgTdGXarT3ewjs/mvb
sgsxggGcMIIBmAIBATBCMD0xEzARBgoJkiaJk/IsZAEZFgNvcmcxGTAXBgoJkiaJ
k/IsZAEZFglydWJ5LWxhbmcxCzAJBgNVBAMMAkNBAgECMAkGBSsOAwIaBQCggbEw
GAYJKoZIhvcNAQkDMQsGCSqGSIb3DQEHATAcBgkqhkiG9w0BCQUxDxcNMDkxMjE2
MTU0NTM0WjAjBgkqhkiG9w0BCQQxFgQUTqRiQxhezJlftad5eZ6u7hNacV0wUgYJ
KoZIhvcNAQkPMUUwQzAKBggqhkiG9w0DBzAOBggqhkiG9w0DAgICAIAwDQYIKoZI
hvcNAwICAUAwBwYFKw4DAgcwDQYIKoZIhvcNAwICASgwDQYJKoZIhvcNAQEBBQAE
gYCMPxJNaR29Yeo/3JWtUTTRq+IlUWHP4bHoZJHQzyFkFPS3fk+9q9KjlTcFY1rT
YbBOUD+QxwU/jlks6Y5PZByIpnWvVy0RujcCzGcMyEY6xKBBkps9X5VuezMB0nbW
xM2k+0e3B7V0KU8fMcO8Ajq9jGn8/hVixbUkyvhq3Xx2Nw==
-----END PKCS7-----
EOP
    jruby_sign = <<EOP
-----BEGIN PKCS7-----
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
9d8f3IBfFoxCvORtzYZKCzW/ZRvEqBO3xJlVuQ==
-----END PKCS7-----
EOP
    store = OpenSSL::X509::Store.new
    store.add_cert(@ca_cert)
    # just checks pubkey's n to avoid certificate expiration.
    # this test is for PKCS#7, not for certificate verification.
    store.verify_callback = proc { |ok, ctx|
      # !! CAUTION: NEVER DO THIS KIND OF NEGLIGENCE !!
      [@ca_cert.public_key.n, @ee1_cert.public_key.n].include?(ctx.current_cert.public_key.n)
      # should return 'ok' here
    }

    p7 = OpenSSL::PKCS7.new(cruby_sign)
    assert(p7.verify([], store))

    p7 = OpenSSL::PKCS7.new(jruby_sign)
    assert(p7.verify([], store))
  end

  def test_detached_sign_compat
=begin
    # how to generate signature
    ca_certs = [@ca_cert]
    flag = OpenSSL::PKCS7::BINARY|OpenSSL::PKCS7::DETACHED
    tmp = OpenSSL::PKCS7.sign(@ee1_cert, @rsa1024, data, ca_certs, flag)
    puts tmp
=end
    cruby_sign = <<EOP
-----BEGIN PKCS7-----
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
zyAlR+YeT270
-----END PKCS7-----
EOP
    jruby_sign = <<EOP
-----BEGIN PKCS7-----
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
RfHXsdxTuA==
-----END PKCS7-----
EOP
    data = "aaaaa\nbbbbb\nccccc\n"
    store = OpenSSL::X509::Store.new
    store.add_cert(@ca_cert)
    # just checks pubkey's n to avoid certificate expiration.
    # this test is for PKCS#7, not for certificate verification.
    store.verify_callback = proc { |ok, ctx|
      # !! CAUTION: NEVER DO THIS KIND OF NEGLIGENCE !!
      [@ca_cert.public_key.n, @ee1_cert.public_key.n].include?(ctx.current_cert.public_key.n)
      # should return 'ok' here
    }

    p7 = OpenSSL::PKCS7.new(cruby_sign)
    assert(!p7.verify([], store))
    assert(p7.verify([], store, data))

    p7 = OpenSSL::PKCS7.new(jruby_sign)
    assert(!p7.verify([], store))
    assert(p7.verify([], store, data))
  end
end

end
