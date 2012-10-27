require "openssl"
require "test/unit"

class TestPKeyRSA < Test::Unit::TestCase
  def test_has_correct_methods
    pkey_methods = OpenSSL::PKey::PKey.instance_methods(false).sort.map(&:intern) - [:initialize]
    assert_equal [:sign, :verify], pkey_methods

    rsa_methods = OpenSSL::PKey::RSA.instance_methods(false).sort.map(&:intern) - [:initialize]
    assert_equal [:d, :d=, :dmp1, :dmp1=, :dmq1, :dmq1=, :e, :e=, :export, :iqmp, :iqmp=, :n, :n=, :p, :p=, :params, :private?, :private_decrypt, :private_encrypt, :public?, :public_decrypt, :public_encrypt, :public_key, :q, :q=, :to_der, :to_pem, :to_s, :to_text], rsa_methods

    assert_equal [:generate], OpenSSL::PKey::RSA.methods(false).map(&:intern)
  end
  
  #iqmp == coefficient
  #e == public exponent
  #n == modulus
  #d == private exponent
  #p == prime1
  #q == prime2
  #dmq1 == exponent2
  #dmp1 == exponent1
  
  def test_can_generate_rsa_key
    OpenSSL::PKey::RSA.generate(512)
  end

  def test_malformed_rsa_handling
    pem = <<__EOP__
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtiU1/UMzIQ1On9OlZGoV
S0yySFYWoXLH12nmP69fg9jwdRbQlb0rxLn7zATbwfqcvGpCcW+8SmdwW74elNrc
wRtbKjJKfbJCsVfDssbbj6BF+Bcq3ihi8+CGNXFdJOYhZZ+5Adg2Qc9Qp3Ubw9wu
/3Ai87+1aQxoZPMFwdX2BRiZvxch9dwHVyL8EuFGUOYId/8JQepHyZMbTqp/8wlA
UAbMcPW+IKp3N0WMgred3CjXKHAqqM0Ira9RLSXdlO2uFV4OrM0ak8rnTN5w1DsI
McjvVvOck0aIxfHEEmeadt3YMn4PCW33/j8geulZLvt0ci60/OWMSCcIqByITlvY
DwIDAQAB
-----END PUBLIC KEY-----
__EOP__
    pkey = OpenSSL::PKey::RSA.new(pem)
    # jruby-openssl/0.6 raises NativeException
    assert_raise(OpenSSL::PKey::RSAError, 'JRUBY-4492') do
      pkey.public_decrypt("rah")
    end
  end

  # http://github.com/jruby/jruby-openssl/issues#issue/1
  def test_load_pkey_rsa
    pem = <<__EOP__
-----BEGIN PRIVATE KEY-----
MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBALRiMLAh9iimur8V
A7qVvdqxevEuUkW4K+2KdMXmnQbG9Aa7k7eBjK1S+0LYmVjPKlJGNXHDGuy5Fw/d
7rjVJ0BLB+ubPK8iA/Tw3hLQgXMRRGRXXCn8ikfuQfjUS1uZSatdLB81mydBETlJ
hI6GH4twrbDJCR2Bwy/XWXgqgGRzAgMBAAECgYBYWVtleUzavkbrPjy0T5FMou8H
X9u2AC2ry8vD/l7cqedtwMPp9k7TubgNFo+NGvKsl2ynyprOZR1xjQ7WgrgVB+mm
uScOM/5HVceFuGRDhYTCObE+y1kxRloNYXnx3ei1zbeYLPCHdhxRYW7T0qcynNmw
rn05/KO2RLjgQNalsQJBANeA3Q4Nugqy4QBUCEC09SqylT2K9FrrItqL2QKc9v0Z
zO2uwllCbg0dwpVuYPYXYvikNHHg+aCWF+VXsb9rpPsCQQDWR9TT4ORdzoj+Nccn
qkMsDmzt0EfNaAOwHOmVJ2RVBspPcxt5iN4HI7HNeG6U5YsFBb+/GZbgfBT3kpNG
WPTpAkBI+gFhjfJvRw38n3g/+UeAkwMI2TJQS4n8+hid0uus3/zOjDySH3XHCUno
cn1xOJAyZODBo47E+67R4jV1/gzbAkEAklJaspRPXP877NssM5nAZMU0/O/NGCZ+
3jPgDUno6WbJn5cqm8MqWhW1xGkImgRk+fkDBquiq4gPiT898jusgQJAd5Zrr6Q8
AO/0isr/3aa6O6NLQxISLKcPDk2NOccAfS/xOtfOz4sJYM3+Bs4Io9+dZGSDCA54
Lw03eHTNQghS0A==
-----END PRIVATE KEY-----
__EOP__
    assert_nothing_raised do
      pkey = OpenSSL::PKey::RSA.new(pem)
      pkey2 = OpenSSL::PKey::RSA.new(pkey.to_pem)
      assert_equal(pkey.n, pkey2.n)
      assert_equal(pkey.e, pkey2.e)
      assert_equal(pkey.d, pkey2.d)
    end
  end

  def test_load_pkey_rsa_enc
    # password is '1234'
    pem = <<__EOP__
-----BEGIN ENCRYPTED PRIVATE KEY-----
MIICoTAbBgkqhkiG9w0BBQMwDgQIfvehP6JEg2wCAggABIICgD7kzSr+xWgdAuzG
cYNkCEWyKF6V0cJ58AKSoL4FQ59OQvQP/hMnSZEMiUpeGNRE6efC7O02RUjNarIk
ciCYIBqd5EFG3OSypK5l777AbCChIkzZHbyE/pIbadr8ZX9C4pkwzPqS0Avzavxi
5s1WDX2GggJkBcQUijqG9QuOZcOvoYbojHPT4tdJq+J6s+0LFas9Jp3a6dYkxtgv
u8Z6EFDZoLGOSVy/jCSMuZAnhoOxUCYqd9FFo2jryV7tQ/CaYAUApAQFTLgBA9qk
4WmyKRpwzIx6EG1pkqulvPXJCcTat9YwllEDVuQ2rKVwDepSl9O7X170Kx1sBecz
mGcfqviU9xwP5mkXO/TLoTZExkHF08Y3d/PTMdxGEDZH37/yRqCIb3Uyqv/jLibM
/s9fm52aWsfO1ndHEhciovlMJvGXq3+e+9gmq1w2TyNQahRc5fwfhwWKhPKfYDBk
7AtjPGfELDX61WZ5m+4Kb70BcGSAEgXCaBydVsMROy0B8jkYgtAnVBb4EMrGOsCG
jmNeW9MRIhrhDcifdyq1DMNg7IONMF+5mDdQ3FhK6WzlFU+8cTN517qA8L3A3+ZX
asiS+rx5/50InINknjuvVkmTGMzjl89nMNrZCjhx9sIDfXQ3ZKFmh1mvnXq/fLan
CgXn/UtLoykrSlobgqIxZslhj3p01kMCgGe62S3kokYrDTQEc57rlKWWR3Xyjy/T
LsecXAKEROj95IHSMMnT4jl+TJnbvGKQ2U9tOOB3W+OOOlDEFE59pQlcmQPAwdzr
mzI4kupi3QRTFjOgvX29leII9sPtpr4dKMKVIRxKnvMZhUAkS/n3+Szfa6zKexLa
4CHVgDo=
-----END ENCRYPTED PRIVATE KEY-----
__EOP__
    assert_nothing_raised do
      pkey = OpenSSL::PKey::RSA.new(pem, '1234')
      pkey2 = OpenSSL::PKey::RSA.new(pkey.to_pem)
      assert_equal(pkey.n, pkey2.n)
      assert_equal(pkey.e, pkey2.e)
      assert_equal(pkey.d, pkey2.d)
    end
  end

  # JRUBY-6622
  def test_load_pkey_rsa_enc_pbes2
    # password is 'password'
    pem = <<__EOP__
-----BEGIN ENCRYPTED PRIVATE KEY-----
MIICxjBABgkqhkiG9w0BBQ0wMzAbBgkqhkiG9w0BBQwwDgQIaYgszaX31yECAggA
MBQGCCqGSIb3DQMHBAij3LmXGCmB8wSCAoDcLnAeXiBugFmwXd3wrvznlKvwHkP2
76lIrTiwDRZOLuaKHdBgNQDJ3NP+UPGdM7YEyNqdfdbN/3cLd0qfzeobuU+c/lGI
aE5pAwlWm5lK9boTsJnCqaDFEgJz2khZF+7RqYQVSG7MTM9SnIRNScLKjhTk7AaF
PD2qSnMVtixw/VfwdzhUknuwP2monLY8Ip/l9abicmBp9HGQ+0WA/nKQLQ/egWG0
S6rrXsH91exaxL7gcZL8jF+Ub7VDt4Hvx1RB/3r12k7AQGsK+TyIrKQFUllSnSq/
eFwBqpLSKWYyGJZlkJzW5MTHyeXqpTvav6T7e2mKZ4GG/a8THoWxLLrKeODFFoWn
LQNOQZ2Axa15E0TdeSkaumsOWPJm5DgFxf/1cRNxhJqYdX68QjWXeNS2SXPZBwlx
HCaAYo6OoCHZQ7O/3MpiT3rUAk30fbSa09VSvrenYi5s5lPieKFt3QZI44uGvi9j
MXyN4fkjzzXasE0xZzf6bQLS6aM+ucyQ8CMv0oAgAndoeKu10Ha4KmdT5dZf3LHj
BUXZDYp3Q5UF6ePyxKBdAqJf4PNKl4+VehYJ4eQ6CIQiSxSuWv9T+2b90PyDuRkz
sB1XZpeDD6dhQuU9GjdwCTyatITcm97ZkbdZEoQiDpiWQB4parTvKLKbD4AbP/+E
08btPFgXNocFUjLb5lB4Y/6RqaQxY7VoaFOPOfPpWPXF26X9Y5y3y+ymXdYFpkhp
wGBGScH+dutQWHoRV1TWUjv9a7CuzUxCX2Hrjooz1BtOnG8CoPA7K43+kvire5jN
529p6u+FtUZPUWLm5L5WHBUECEtJGw3ImjosX1HtoM/rW34XDmMHuN0u
-----END ENCRYPTED PRIVATE KEY-----
__EOP__
    assert_nothing_raised do
      pkey = OpenSSL::PKey::RSA.new(pem, 'password')
      pkey2 = OpenSSL::PKey::RSA.new(pkey.to_pem)
      assert_equal(pkey.n, pkey2.n)
      assert_equal(pkey.e, pkey2.e)
      assert_equal(pkey.d, pkey2.d)
    end
  end

  # jruby-openssl/0.6 causes NPE
  def test_generate_pkey_rsa_empty
    assert_nothing_raised do
      OpenSSL::PKey::RSA.new.to_pem
    end
  end

  def test_generate_pkey_rsa_length
    assert_nothing_raised do
      OpenSSL::PKey::RSA.new(512).to_pem
    end
  end

  def test_generate_pkey_rsa_to_text
    assert_match(
      /Private-Key: \(512 bit\)/,
      OpenSSL::PKey::RSA.new(512).to_text
    )
  end

  def test_load_pkey_rsa
    pkey = OpenSSL::PKey::RSA.new(512)
    assert_equal(pkey.to_pem, OpenSSL::PKey::RSA.new(pkey.to_pem).to_pem)
  end

  def test_load_pkey_rsa_public
    pkey = OpenSSL::PKey::RSA.new(512).public_key
    assert_equal(pkey.to_pem, OpenSSL::PKey::RSA.new(pkey.to_pem).to_pem)
  end

  def test_load_pkey_rsa_der
    pkey = OpenSSL::PKey::RSA.new(512)
    assert_equal(pkey.to_der, OpenSSL::PKey::RSA.new(pkey.to_der).to_der)
  end

  def test_load_pkey_rsa_public_der
    pkey = OpenSSL::PKey::RSA.new(512).public_key
    assert_equal(pkey.to_der, OpenSSL::PKey::RSA.new(pkey.to_der).to_der)
  end

  def test_load_rsa_des_encrypted
    password = 'pass'
    pkey = OpenSSL::PKey::RSA.generate(1024)
    cipher = OpenSSL::Cipher::Cipher.new('des-cbc')
    pem = pkey.to_pem(cipher, password)
    assert_equal(pkey.n, OpenSSL::PKey::RSA.new(pem, password).n)
  end

  def test_load_rsa_3des_encrypted
    password = 'pass'
    pkey = OpenSSL::PKey::RSA.generate(1024)
    cipher = OpenSSL::Cipher::Cipher.new('des-ede3-cbc')
    pem = pkey.to_pem(cipher, password)
    assert_equal(pkey.n, OpenSSL::PKey::RSA.new(pem, password).n)
  end

  def test_load_rsa_aes_encrypted
    password = 'pass'
    pkey = OpenSSL::PKey::RSA.generate(1024)
    cipher = OpenSSL::Cipher::Cipher.new('aes-128-cbc')
    pem = pkey.to_pem(cipher, password)
    assert_equal(pkey.n, OpenSSL::PKey::RSA.new(pem, password).n)
  end

  CRUBY_DES_RSA_PEM = <<END
-----BEGIN RSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: DES-CBC,D22ABA5D7A345AAF

uSRJRBARry1L2DUZwdLkQ0JA4riJ9CWnsA+Y6M76dznyzJCLK/rLyQSLxZefVWVk
CzkRMhi4n4NrSS1hB7qUlphX6y67O5Rv/O0S4SYa3d87hDJKqNpR47gfj35efNUk
izIb4rHoVgolRNm/L4joChuIrKqHkQnkusNxp9wReuNT/B2qPbqqEGxiDCcv8jni
fq1ifvkJyTlEKnmrdml65fhjHHDuNENVAXVrsPsVJXI6kk7PahJk24rn29qPOAlJ
d+E6Q6YDgRM1MDrEQmaXFzNJi8Mco1712LjXIYH9+vkJ5GnXrIk8XrAmY2E4ERgQ
XdFx9J6Qncnjj/y18H9WilvoaN4hJiE22OqNI/PeK+nYeeYnxzu2qc2E5QVyjCqA
N2nTwx0ZtZRxFakP8CMyrVCn7BVYdIF3ISgX7RT+GwkujKwWAO0xrjkMc3y3Pv2R
9MG5PzeFe4EuuwkjN0rWB8ra4HCkaPxk
-----END RSA PRIVATE KEY-----
END

  JRUBY_DES_RSA_PEM = <<END
-----BEGIN RSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: DES-CBC,14f319c66f99b413

JTTxyeEPF9HcOyyLQ0Y+oO91RaVYIoqppxpfKaULXBZwHN8pGSrHFPliCgahgEEC
aF39H6/69KrEKw7h8XYKrFK+sinBYd975qWfspexmDHTyMcN7MPBPpbnV5Ha2z+m
weenO/2rAzsOxNa6dRBmdCyHYODJrSDq71h6W7255t79E4Tjr1u72/lHPVTO2r9a
sv0AhkfM5G95n+qamgNYOYjWODKFaSA6vN4sB08UQZBN7EJeE6Ouv5hw8E0uooLu
10iaBJUWkYF9v50+Z+cVd890VlUiQZihybQ8dETHtBtJ6moaMxBzQjkXjGJf6vd+
h0YPQOIg0YLwxg+aIC4L+Sh/11DrXSeb6j/7d0HfqGW060AC1b7crits71v2vEGe
+9kGgboShossQLoOuFicETEo5UgyhWfJ+ftvKM5DgYfsKJNsegSH9WwISFNWGSsY
njt38WTtkSxDQmm1G+VGfdfOIUQWohI9Prol2qodhsN6SvJgd0n9nVYi388bh1IR
23jnX9eayTrCm6Ff9EBVIroG8wVr0Qqme36o0GDtmCXtuBY9F3utr/8n03lTBuW7
E/Cx4XKlTtA7oBQjDz/ecbSGZXIROQmZh2f4wX6JeDhQH93InvM5DP7nN7qq+bxp
aZPRS3QFj0XlcF/QTI5r/GqT+DAWZ4ZXGtuKUg2yEjCNz6+r8xW9zOFSFos+773U
MVitdHtRLjfRVV6PT6VyThaHI56f6T9PC7KHgX8e/YcA8cAR0IoOjVsy1Xy0KWn7
CLxLcO6qbYRDyoFEpMrYq/O2beA1BH28BXnDkODbOje/lbvL9sSaHg==
-----END RSA PRIVATE KEY-----
END

  CRUBY_3DES_RSA_PEM = <<END
-----BEGIN RSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: DES-EDE3-CBC,F4E217EFFC0C21F0

KblUxUrSn2mJlzZUyZ/QBBWdhDkPwO4bzH6y6XxUmVEhv3DgiiWiOccQoE9s06Gt
LguPArk2W61DfksDH7ESeVd5syS+6LVmuVXePrXlef45flNwmdFHXgy+OVM9uLHT
y1AvHs6f3NLsDlf+ho8yoKgCTTBWf9lwAxw6EO9iKqUWY4ofRjSefiGxwhIW77Vc
MIktK9wMcZL9d8mINqQEY/Pz7pg70bLp3uZlljNFo6i8OA2SnitTOmU97dyknbee
+HDzrFFGdwsCg6nJdnjDsOXYeXDW2sul8g258ipUEVpkByWZ7Vy3vpAZKwO2dIrj
oEr/6glDvOKd7w2U5DIqmuWo5zvtCBu1taonyyaJKRoxsmJO9PTbDhz38WU3wcbf
9AQxDlMY//N5hSICrcnIaXsjMTxiJrERHLPjW+auYSRl7E9qKh4KqjDHsJ2/LyTE
dQ7IaJwE9xygO1hVHLJze1pZs+xHsxdR
-----END RSA PRIVATE KEY-----
END

  JRUBY_3DES_RSA_PEM = <<END
-----BEGIN RSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: DES-EDE3-CBC,102c5e7fdd051c1a

u19K4AySBL8KAM9DSLsSJSuxtKLQgmPiawpPRBpYjMITxw2NQtpZMYJKLZOtx/uU
kVjbwzJhhr51ciBF2HztLLvrpmh9xPybLZFh4Ew7/yMMOir+GyO3G8pf3sL5roAA
+sR4VERTiK6KWdblZQCYOmyJDiffjWBfzSz6MlLdjjTZr+NBQ6d0vGFGdLE1662Y
XpjcG6nw3fpUhdTXhXJDYneSBD30P6sfSxzPY0G7TLNU6vv9qlaeZ3nksfY/2Li1
aOSzrYtZe9+uzqAd9KxNNO7Eya3GAgdhErwVQGIU/cmtNybYdeaEhOtd80Vx8qY0
l9Lz/FtKdbxxoSAFhpRupRY2cFC80LJ2e9hp+TRkYp2k9a9UuPMrTYmK6GoYsPRC
SqnL9xIlCrByxhQ3vJaogSseAFlFmLxb+E61cQxCHfc5oBM9XY3e60Cirfsx/POG
xvTPXBLS1SuLybLh68wZDpLcqTFk0zG0oSchUt6x5JqK4OImKnI2VpXZ1H7j/rSU
ghUJvDcvAiMwiqep/81Ue9KIrP+ouluk81njjFTA636Dx23PdPq0dyBwi4iF+WOf
A4EhhISttdb+ZSmS9kPDYn2a79Qvds29yOgEB2OwKIstDzlBNO85CA+E3JbOndBq
+zpQklVLA+dWw0dTIbwI5xYKGb7HJJPOpBC0aWcnudr4rda2b2WIFPzH/PTAoIhJ
kBuwNoLBA38qh+T6Se66hLWzWyt3yPWaH81KW0WOaHW68LC572+qq7hrsOHp7ya6
C4/lMsQ5zUyRI0fwpnSJv1RjYemNJMV+oUiHFDKN5jQ=
-----END RSA PRIVATE KEY-----
END

  CRUBY_AES_RSA_PEM = <<END
-----BEGIN RSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: AES-128-CBC,E6FB0DECDE6009CF6EEE61885679905E

254YnHYZVo139fw1jbztY/v7BbaaCoYAMC27q6NF4k5Qocfe12HYWD/pgpe1YKbX
8fmfBhKFpjqTAr8emp0HOOw8VjfcaITiN4MvrnyhT7Ir4yjJjO5mUQVBZQha+i7P
CFTsuZhDF1iEkxkyVsHQ56iWT+SL7d0uvWW2eVNZ0L/vRVrdffNG1m+92xIm66Fj
0/fMudTSUYJvad2Isj64i1DWKxo6gfxgmukLCwj4DnmjVQvOH/VQT7nUe9oqxCuy
VUC+c2FAZgTr+2vSYdpPnU/u//DKjqvDgZHDkRIKDNNDv1yyVRY7giuOfFE9GqbE
wQTwaLdJYnu9I4HaS8tWCLHgNYRKbvNcTRX7CMxlV82ndn7aKoIDb/x8O5frObEF
2gMnv4H5mxrtHCJVk0ivQf3BgZWTdom/YEBr56RC8R/8y5dWsaGhIbXr9kNW0Lom
50oQToYBbM5ulLJxYXV3tA==
-----END RSA PRIVATE KEY-----
END

  JRUBY_AES_RSA_PEM = <<END
-----BEGIN RSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: AES-128-CBC,5c6a1d2d24302e15a23bb77483db3966

2IBvwJDEw3CkZVMp7/ML0qiozHqmkmkZ+XEe3YD8EV8te7rO7zRVNCW+NOxfqUhu
TrP5gXNLK2rUrhBairruFz6iXWndtkGs1o8LL2X+dCSRbFpkVOo6d7IEpRi5xJDJ
MK1LzWLb1I3EUxlEY/NkpfIaULxldix9QCXejqdlHyuRvDEO11HwT2AO1ND/Pir9
510nnDrBF0oGy/y9n5Quw/3F30CLyNUoQ+yb05unRt3MXQ40Qh4qIxE2bEsT5bPV
YHQ7Ls4ETxSyqVETpWfghAl2pipU2S6ZhCcPETnv6nfjC72uk/dFRuaMsve4IUTu
AnxZCKQ6ICaExlgUzY97lEIj3nJDY5PbDARqE1EQt24nS4wHq+5hrMumDLaiYcFu
QqfDYDXJiZt0gUYXlHwezNyv+iH4Y5xhko7fQn0yFhp4ud748zZJVYzULJCMGVWT
ObQ6I5JE2UoTeTHl/UfE3UYqKoBPnnElsJkTFzdql8zBS5PGAHgrF06Wg/jp9iCG
h0BXTnPw5jBVF8YF15F9oWt6hzLVrBfihTCi1+KxKPYDCoKUVGyeaSmCRTwu/Shp
WJpKokBtazrML5gXE2rTho3yHOpiwv/3HLLGKpVxBZk/w+quo5o1dQbuNcdVkXUy
uvMFYJGBMXhpFHxWOnrHe5EWmlergjbH6FtDnZ6q9mGKX3Zv0mj16rGTwaC8ojFJ
klvWNuzEyQRT2OJNbiJ+w8v4yF9nKcoNnWaLwsTXkdfrPkrUEyG03aNj5UVkJ8cq
jfdOf8b35frgdBJrFb1fNxXUDiOmHxBeffidJ7LEHD0zvh8yke9iqCRquRbQw4ua
-----END RSA PRIVATE KEY-----
END

  def test_load_rsa_des_encrypted_compat
    password = 'pass'
    assert_equal(171121679472900958735046240032013822902814135418044632926746858725279957006460484359346082493980272450155346042705805047522822137075000873718614673206839485927470809962483274240113443184049955325778842883884472730809338103721103527723371013427831682847229398280665281140996554391864952366240593334371598093357, OpenSSL::PKey::RSA.new(CRUBY_DES_RSA_PEM, password).n)
    assert_equal(95186390926176289293448721787465460008681849005943627766414746880750829275325362105354699806095724614833850511391997530422798534550053092884697848715098905298125981139872096315909555296044739053126836027629923201408465604387441696201951345435727705264545384652266958892017406564235498456780610723946372077161, OpenSSL::PKey::RSA.new(JRUBY_DES_RSA_PEM, password).n)
  end

  def test_load_rsa_3des_encrypted_compat
    password = 'pass'
    assert_equal(159891081887610779337613110093558981667630640397086024796277575756132362092191306947719745882625669532365621381440788450544049011459945371880102089907577418294502920328095107337128188112059054500699255092076505828498357575128536918367005034864200777995404401769611478230798677758186716282278489076140546075717, OpenSSL::PKey::RSA.new(CRUBY_3DES_RSA_PEM, password).n)
    assert_equal(132634270546428248975101416587398035596645524786882670408365466475894396526829329830349264854502825314377251520406282650983030314030180184335663605776194424399755874220996106852804569548140575796496611429489809370931661876941690628312711482612941088835232077828296662784479160183720585553373956579796213039707, OpenSSL::PKey::RSA.new(JRUBY_3DES_RSA_PEM, password).n)
  end

  def test_load_rsa_aes_encrypted_compat
    password = 'pass'
    assert_equal(152164605304862839347386799863418642272176047421496804966498177563653853015174053675480323318393334405555865282832768092329962715390636176200784476670180320842603832200246642618431746763085706624847469867394029459008113763032679106133646348160741974785288005086128311619599829066360227829804048705177001887963, OpenSSL::PKey::RSA.new(CRUBY_AES_RSA_PEM, password).n)
    assert_equal(120124464337037052596736192517844019014106857114253451267066925743499301063116479220243836708739023567649536721432121286547319736881999977669587689339777273865695637895993003754843628769179367393259439036333237000420047538052601743699164582419555307462460610926941760286052729693393703143580060262101504625743, OpenSSL::PKey::RSA.new(JRUBY_AES_RSA_PEM, password).n)
  end
end
