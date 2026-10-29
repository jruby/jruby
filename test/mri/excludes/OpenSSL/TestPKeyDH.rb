exclude :test_key_exchange, 'needs investigation'
exclude :test_new_break, 'needs investigation'
exclude :test_new_break_on_non_fips, "hangs (tested on macos M1)"
exclude :test_params, "new failure with Ruby 4.0 tests, tested on MacOS, https://github.com/jruby/jruby/issues/9271"
