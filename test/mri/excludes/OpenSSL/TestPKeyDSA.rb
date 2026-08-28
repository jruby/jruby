exclude :test_DSAPrivateKey, "new failure with Ruby 4.0 tests, tested on MacOS, https://github.com/jruby/jruby/issues/9271, IllegalArgumentException: Bad sequence size: 3"
exclude :test_DSAPrivateKey_encrypted, "new failure with Ruby 4.0 tests, tested on MacOS, https://github.com/jruby/jruby/issues/9271, IllegalArgumentException: Bad sequence size: 3"
exclude :test_PUBKEY, "new failure with Ruby 4.0 tests, tested on MacOS, https://github.com/jruby/jruby/issues/9271, IllegalArgumentException: Bad sequence size: 3"
exclude :test_dup, 'passes except for setting (invalid) `key2.p + 1` as validation happens early'
exclude :test_new_break, 'needs investigation'
exclude :test_new_empty, "PKeyError expected, got DSAError - error class hierarchy mismatch with Ruby 4.0 tests"
exclude :test_sign_verify_raw, "work in progress"
