exclude :test_RSAPrivateKey_encrypted, "PKeyError expected, got RSAError - error class hierarchy mismatch with Ruby 4.0 tests"
exclude :test_encrypt_decrypt, "work in progress"
exclude :test_encrypt_decrypt_legacy, "work in progress"
exclude :test_new_break, 'needs investigation'
exclude :test_no_private_exp, "new failure with Ruby 4.0 tests, tested on MacOS, https://github.com/jruby/jruby/issues/9271"
exclude :test_params, "new failure with Ruby 4.0 tests, tested on MacOS, https://github.com/jruby/jruby/issues/9271"
exclude :test_pem_passwd, "work in progress"
exclude :test_private_encoding_encrypted, "work in progress"
exclude :test_sign_verify_memory_leak, 'depends on GC.start forcing GC and changing memory size (e.g. RSS)'
exclude :test_sign_verify_pss, "PKeyError expected, got RSAError - error class hierarchy mismatch with Ruby 4.0 tests"
exclude :test_sign_verify_raw, "work in progress"
exclude :test_sign_verify_raw_legacy, "PKeyError expected, got RSAError - error class hierarchy mismatch with Ruby 4.0 tests"
