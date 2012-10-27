require "openssl"
require "test/unit"

class TestPKeyDSA < Test::Unit::TestCase
  def test_can_generate_dsa_key
    OpenSSL::PKey::DSA.generate(512)
  end

  # jruby-openssl/0.6 causes NPE
  def test_generate_pkey_dsa_empty
    assert_nothing_raised do
      OpenSSL::PKey::DSA.new.to_pem
    end
  end

  # jruby-openssl/0.6 ignores fixnum arg => to_pem returned 65 bytes with 'MAA='
  def test_generate_pkey_dsa_length
    assert(OpenSSL::PKey::DSA.new(512).to_pem.size > 100)
  end

  # jruby-openssl/0.6 returns nil for DSA#to_text
  def test_generate_pkey_dsa_to_text
    assert_match(
      /Private-Key: \(512 bit\)/,
      OpenSSL::PKey::DSA.new(512).to_text
    )
  end

  def test_load_pkey_dsa
    pkey = OpenSSL::PKey::DSA.new(512)
    assert_equal(pkey.to_pem, OpenSSL::PKey::DSA.new(pkey.to_pem).to_pem)
  end

  def test_load_pkey_dsa_public
    pkey = OpenSSL::PKey::DSA.new(512).public_key
    assert_equal(pkey.to_pem, OpenSSL::PKey::DSA.new(pkey.to_pem).to_pem)
  end

  def test_load_pkey_dsa_der
    pkey = OpenSSL::PKey::DSA.new(512)
    assert_equal(pkey.to_der, OpenSSL::PKey::DSA.new(pkey.to_der).to_der)
  end

  def test_load_pkey_dsa_public_der
    pkey = OpenSSL::PKey::DSA.new(512).public_key
    assert_equal(pkey.to_der, OpenSSL::PKey::DSA.new(pkey.to_der).to_der)
  end

  def test_load_pkey_dsa_net_ssh
    blob = "0\201\367\002\001\000\002A\000\203\316/\037u\272&J\265\003l3\315d\324h\372{\t8\252#\331_\026\006\035\270\266\255\343\353Z\302\276\335\336\306\220\375\202L\244\244J\206>\346\b\315\211\302L\246x\247u\a\376\366\345\302\016#\002\025\000\244\274\302\221Og\275/\302+\356\346\360\024\373wI\2573\361\002@\027\215\270r*\f\213\350C\245\021:\350 \006\\\376\345\022`\210b\262\3643\023XLKS\320\370\002\276\347A\nU\204\276\324\256`=\026\240\330\306J\316V\213\024\e\030\215\355\006\037q\337\356ln\002@\017\257\034\f\260\333'S\271#\237\230E\321\312\027\021\226\331\251Vj\220\305\316\036\v\266+\000\230\270\177B\003?t\a\305]e\344\261\334\023\253\323\251\223M\2175)a(\004\"lI8\312\303\307\a\002\024_\aznW\345\343\203V\326\246ua\203\376\201o\350\302\002"
    pkey = OpenSSL::PKey::DSA.new(blob)
    assert_equal(blob, pkey.to_der)
  end

  def test_load_dsa_des_encrypted
    password = 'pass'
    pkey = OpenSSL::PKey::DSA.generate(512)
    cipher = OpenSSL::Cipher::Cipher.new('des-cbc')
    pem = pkey.to_pem(cipher, password)
    assert_equal(pkey.g, OpenSSL::PKey::DSA.new(pem, password).g)
  end

  def test_load_dsa_3des_encrypted
    password = 'pass'
    pkey = OpenSSL::PKey::DSA.generate(512)
    cipher = OpenSSL::Cipher::Cipher.new('des-ede3-cbc')
    pem = pkey.to_pem(cipher, password)
    assert_equal(pkey.g, OpenSSL::PKey::DSA.new(pem, password).g)
  end

  def test_load_dsa_aes_encrypted
    password = 'pass'
    pkey = OpenSSL::PKey::DSA.generate(512)
    cipher = OpenSSL::Cipher::Cipher.new('aes-128-cbc')
    pem = pkey.to_pem(cipher, password)
    assert_equal(pkey.g, OpenSSL::PKey::DSA.new(pem, password).g)
  end

  CRUBY_DES_DSA_PEM = <<END
-----BEGIN DSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: DES-CBC,55669A6757B8752A

0DI84kfjNWOmckdBhvraem9LZqx++JRAD0fUB7J2ymLLMKS2wFq32AA3XD6K9kNP
Ud1idm0UMtJjxFFtT02ue7USlaSHIb+INbf4m5N/I54cOoMXXdOEFeecWlmDhfMP
zSA/6zM1L0iUKXaR30yRtslwZpGi3ZlriLkL6HbZFjBRUk93AL01oVHHIW3OPqLq
Mql0jpr9VDLi1OjkIbrRilZKHTx/C7DCeBfwhPS3KUhkyGTqQzTwa6TpA5fiL0vy
OXwDr9GqZCUq5sk3Psu+2yrI+dbzUFh+va+0p2EzVnOb7p6oyIhXTyyeqZVy+KRz
qK2rRxrZmQv9hJ59G1kJ/A==
-----END DSA PRIVATE KEY-----
END

  JRUBY_DES_DSA_PEM = <<END
-----BEGIN DSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: DES-CBC,06b4e0b42bf90db9

mroGVjL7pb1BSJDtaksk3If8XidwLgv0DJsQcmwG8gc55eBxF65TDmAFZlc1vAp8
YlkQx84ZCWufiTlWzeZR/giEPeps0nbzkaNN4tWOEN3ILF7TKfaqFdtJgEwLXMJ8
3L5jLZDsESmXpGY51RGp7LXYENoS1WwnJ+ke9kBty/IpFVUYyRtRZ2wpoFqWKfjV
rAlYqJLme+PQL1SXoPl3HOMr7NcjaEr9yYeq8mPCAg8YEW1ckyH0Z6j9ZK0spdtM
Cam9mftFK1gC4wqZbCSM3bRCrqiVfMr8uWIIZVpBwm1skYh8+yLlG1m0xhTcmMEq
Vo/ZKPN3/eLMl2nx5Td8ew==
-----END DSA PRIVATE KEY-----
END

  CRUBY_3DES_DSA_PEM = <<END
-----BEGIN DSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: DES-EDE3-CBC,75267DEE9DB19A15

rWX5EyFJ4n64oHadHhSKmW/wJWTeoA5K/CxdlAzh9sGzNtbJB1qevd7iV3HouQiZ
iGeA0b8tJWI8NJZFjYsQyNZgFemnpQ/Nz0kYXR/z0TpJzMHepyDn+3n7WBupRM+J
aEmgwSJSiwEDYDInscdIdTNW/zfXt8+wKf72rttV8ocTAHkIbBew1YFQK+hM2OZ5
MubNrFaiSwTJisGU0Vc5sAcaq60ZyfI730LN1QSA9FRWn/O8od3O+Ri7w8K2qRXV
R/z33GsXqt1dkaQVwH4yFUaUect/fgXfuijZaJrBY7ZT9oBPxsBrAxVIATf2bfQY
Vk30SWL9mazMmDcgjD32hg==
-----END DSA PRIVATE KEY-----
END

  JRUBY_3DES_DSA_PEM = <<END
-----BEGIN DSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: DES-EDE3-CBC,d795fb005ae84cd7

iSSWTjX84CqfaBGlds7NNBDNaAIvnbv6WI8uh2Mv4KJ5PLvpAAb2bxQUNLCAHlvP
gqWdUbNvC9IVGNfE9exYdW/1bevp9wjcjv6dK+1dH9H+WxFqZtnsuH0fOw5EWrXY
+yB0SQQo44UVqy/w+2SbYrYEQ/MAA4ebYqd8ubdi3qFoSjfuUnh/vdAakePqM82a
Vb4w35j7ihXr/RrtbP+bCW5rVHPUCKOvBy9XD0UG8wtEQH0wUmJnELWoWM6/xLS8
rFBX3EOYR2nItP5XQyHO49RRT3vzLY04cCYIa5/LhBBRbCSWi6oQaydxq6/buGff
z//DKy1e6FpeUIFgyjkldQ==
-----END DSA PRIVATE KEY-----
END

  CRUBY_AES_DSA_PEM = <<END
-----BEGIN DSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: AES-128-CBC,5F8642885141BB27B7D10E089006BF22

aBYZIZw67pJFf8voJK2PH6dT2npXL2sGgG4NStgrClAowFF3o0v+p7eAzKwIsfex
LSE60JAPtD/ooqGDoGkJqT3VXncpNw0B9dR3l2Z2j79ZCNuH64z8PnQKDCLjIVKj
yhqUsifokc/s7Vd96uaIfR8z3S3go+/96gbIr6p34FLD6N2Pi06t0XrN70msURaX
Q84edbPto/5Zl8zjVFuV+nvGidozytJ1b6txzyatKeCBQO8doZHihXpW32iyUkJ8
1H0047SzKhswP7Xxwob7ukcrPcn2ehPG3+bXC1w7J47jsIi6ihvHKMl6KfgPwVgu
a8/ecsqcdOGUXmtRJ2l3dw==
-----END DSA PRIVATE KEY-----
END

  JRUBY_AES_DSA_PEM = <<END
-----BEGIN DSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: AES-128-CBC,2327f11cbbaaf0a5f9ec992ae04677b1

YSG7L3SO9/Z+IgDSGf30oXaqUlwc+/AzKeMfqBCxs0qGP2lMELfHnwz/mwxlbepP
W4zDqgTV88Fvg8PpQVsFamrEWjIy2Bldztc+8Aj8ndBON3vqDkSDk9/FIVFnKCsQ
xXR/6ixhFVwBsWGJeGSM1PRPcX74JLBbUlY0vzZPk/uUcg7dQO/oGskOlBo/oLnS
xVXLvRo2CR+3ydHE9iF+y4Y/66f7uccbIAyUDgaurZLf/3MRihAydlkEUQriK88Q
z1fEb60qcDl8N4bQD2HOILUyPcwvT7OZsn01HmfwxtxMvMsfeIO2NnUUqmpCXawW
Cj7lDxD1ioXbyqmJyucyVg==
-----END DSA PRIVATE KEY-----
END

  def test_load_dsa_des_encrypted_compat
    password = 'pass'
    assert_equal(2601835381435922300807529879321911722473209798319079426284347845245761126085714618755566626587428986614532580618831696049079493874672683842235950274336046, OpenSSL::PKey::DSA.new(CRUBY_DES_DSA_PEM, password).g)
    assert_equal(5421644057436475141609648488325705128047428394380474376834667300766108262613900542681289080713724597310673074119355136085795982097390670890367185141189796, OpenSSL::PKey::DSA.new(JRUBY_DES_DSA_PEM, password).g)
  end

  def test_load_dsa_3des_encrypted_compat
    password = 'pass'
    assert_equal(9936878837924433385259546254653845785685748445545516924244160189763997725813126701147586419234217178553350132394993861862023343427937351327849016782576411, OpenSSL::PKey::DSA.new(CRUBY_3DES_DSA_PEM, password).g)
    assert_equal(5421644057436475141609648488325705128047428394380474376834667300766108262613900542681289080713724597310673074119355136085795982097390670890367185141189796, OpenSSL::PKey::DSA.new(JRUBY_3DES_DSA_PEM, password).g)
  end

  def test_load_dsa_aes_encrypted_compat
    password = 'pass'
    assert_equal(4237872605088828155551665609293446566126489611054397621156235339811966387210852049465706387691172976146024902239923506580469901654107741400464080284215372, OpenSSL::PKey::DSA.new(CRUBY_AES_DSA_PEM, password).g)
    assert_equal(5421644057436475141609648488325705128047428394380474376834667300766108262613900542681289080713724597310673074119355136085795982097390670890367185141189796, OpenSSL::PKey::DSA.new(JRUBY_AES_DSA_PEM, password).g)
  end
end
