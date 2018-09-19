exclude :test_ASET_limits, "expected behavior; JRuby can only do int range offsets into a string, so error reflects that"
exclude :test_crypt, "does not raise as expected"
exclude :test_setter, "does not raise as expected"
exclude :test_uplus_minus, "only seems to fail in a full test run"
