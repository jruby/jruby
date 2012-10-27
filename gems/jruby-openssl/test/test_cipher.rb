begin
  require "openssl"
rescue LoadError
end

require "test/unit"

class TestCipher < Test::Unit::TestCase
  def test_keylen
    cipher = OpenSSL::Cipher::Cipher.new('DES-EDE3-CBC')
    # must be 24 but it returns 16 on JRE6 without unlimited jurisdiction
    # policy. it returns 24 on JRE6 with the unlimited policy.
    assert_equal(24, cipher.key_len)
  end

  def test_encrypt_takes_parameter
    enc = OpenSSL::Cipher::Cipher.new('DES-EDE3-CBC')
    enc.encrypt("123")
    data = enc.update("password")
    data << enc.final
  end

  IV_TEMPLATE  = "aaaabbbbccccddddeeeeffffgggghhhhiiiijjjjj"
  KEY_TEMPLATE = "aaaabbbbccccddddeeeeffffgggghhhhiiiijjjjj"

  # JRUBY-1692
  def test_repeated_des
    do_repeated_test(
                     "des-ede3-cbc",
                     "foobarbazboofarf",
                     ":\022Q\211ex\370\332\374\274\214\356\301\260V\025",
                     "B\242\3531\003\362\3759\363s\203\374\240\030|\230"
                     )
  end

  # JRUBY-1692
  def test_repeated_aes
    do_repeated_test(
                     "aes-128-cbc",
                     "foobarbazboofarf",
                     "\342\260Y\344\306\227\004^\272|/\323<\016,\226",
                     "jqO\305/\211\216\b\373\300\274\bw\213]\310"
                     )
  end

  def test_rc2
    do_repeated_test(
                     "RC2",
                     "foobarbazboofarf",
                     "\x18imZ\x9Ed\x15\xF3\xD6\xE6M\xCDf\xAA\xD3\xFE",
		     "\xEF\xF7\x16\x06\x93)-##\xB2~\xAD,\xAD\x82\xF5"
		    )
  end

  def test_rc4
    do_repeated_test(
                     "RC4",
                     "foobarbazboofarf",
                     "/i|\257\336U\354\331\212\304E\021\246\351\235\303",
                     "\020\367\370\316\212\262\266e\242\333\263\305z\340\204\200"
		    )
  end

  def test_cast
    do_repeated_test(
                     "cast-cbc",
                     "foobarbazboofarf",
                     "`m^\225\277\307\247m`{\f\020fl\ry",
                     "(\354\265\251,D\016\037\251\250V\207\367\214\276B"
		    )
  end

  # JRUBY-4326 (1)
  def test_cipher_unsupported_algorithm
    assert_raise(OpenSSL::Cipher::CipherError) do
      cipher = OpenSSL::Cipher::Cipher.new('aes-xxxxxxx')
    end
  end

  # JRUBY-4326 (2)
  def test_cipher_unsupported_keylen
    bits_128 = java.lang.String.new("0123456789ABCDEF").getBytes()
    bits_256 = java.lang.String.new("0123456789ABCDEF0123456789ABCDEF").getBytes()

    # AES128 is allowed
    cipher = OpenSSL::Cipher::Cipher.new('aes-128-cbc')
    cipher = OpenSSL::Cipher::Cipher.new('AES-128-CBC')
    cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
    key_spec = javax.crypto.spec.SecretKeySpec.new(bits_128, "AES")
    iv_spec = javax.crypto.spec.IvParameterSpec.new(bits_128)
    assert_nothing_raised do
      cipher.init(javax.crypto.Cipher::ENCRYPT_MODE, key_spec, iv_spec)
    end

    # check if AES256 is allowed or not in env policy
    cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
    key_spec = javax.crypto.spec.SecretKeySpec.new(bits_256, "AES")
    allowed = false
    begin
      cipher.init(javax.crypto.Cipher::ENCRYPT_MODE, key_spec, iv_spec)
      allowed = true
    rescue
    end

    # jruby-openssl should raise as well?
    # CRuby's openssl raises exception at initialization time.
    # At this time, jruby-openssl raises later. TODO
    cipher = OpenSSL::Cipher::Cipher.new('aes-256-cbc')
    cipher.encrypt
    cipher.padding = 0
    if allowed
      assert_nothing_raised(OpenSSL::Cipher::CipherError) do
        cipher.pkcs5_keyivgen("password")
      end
    else
      assert_raise(OpenSSL::Cipher::CipherError) do
        cipher.pkcs5_keyivgen("password")
      end
    end
  end

  def test_iv_length_auto_trim_JRUBY_4012
    e1 = e2 = nil
    plain = 'data'
    des = OpenSSL::Cipher::Cipher.new("des-ede3-cbc")
    des.encrypt
    des.key = '0123456789abcdef01234567890'
    des.iv = "0" * (128/8) # too long for DES which is a 64 bit block
    assert_nothing_raised do
      e1 = des.update(plain) + des.final  
    end
    des = OpenSSL::Cipher::Cipher.new("des-ede3-cbc")
    des.encrypt
    des.key = '0123456789abcdef01234567890'
    des.iv = "0" * (64/8) # DES is a 64 bit block
    e2 = des.update(plain) + des.final  
    assert_equal(e2, e1, "JRUBY-4012")
  end

  # JRUBY-5125
  def test_rc4_cipher_name
    assert_equal("RC4", OpenSSL::Cipher::Cipher.new("rc4").name)
  end

  # JRUBY-5126
  def test_stream_cipher_reset_should_be_ignored
    c1 = "%E\x96\xDAZ\xEF\xB2$/\x9F\x02"
    c2 = ">aV\xB0\xE1l\xF3oyL\x9B"
    #
    cipher = OpenSSL::Cipher::Cipher.new("RC4")
    cipher.encrypt
    cipher.key = "\0\1\2\3" * (128/8/4)
    str = cipher.update('hello,world')
    str += cipher.final
    assert_equal(c1, str)
    #
    cipher.reset
    cipher.iv = "\0" * 16
    str = cipher.update('hello,world')
    str += cipher.final
    assert_equal(c2, str) # was equal to c1 before the fix
  end

  private
  def do_repeated_test(algo, string, enc1, enc2)
    do_repeated_encrypt_test(algo, string, enc1, enc2)
    do_repeated_decrypt_test(algo, string, enc1, enc2)
  end
  
  def do_repeated_encrypt_test(algo, string, result1, result2)
    cipher = OpenSSL::Cipher::Cipher.new(algo)
    cipher.encrypt

    cipher.padding = 0
    cipher.iv      = IV_TEMPLATE[0, cipher.iv_len]
    cipher.key     = KEY_TEMPLATE[0, cipher.key_len]

    assert_equal result1, cipher.update(string)
    assert_equal "", cipher.final

    assert_equal result2, cipher.update(string) + cipher.final
  end

  def do_repeated_decrypt_test(algo, result, string1, string2)
    cipher = OpenSSL::Cipher::Cipher.new(algo)
    cipher.decrypt

    cipher.padding = 0
    cipher.iv      = IV_TEMPLATE[0, cipher.iv_len]
    cipher.key     = KEY_TEMPLATE[0, cipher.key_len]

    assert_equal result, cipher.update(string1)
    assert_equal "", cipher.final

    assert_equal result, cipher.update(string2) + cipher.final
  end
end
