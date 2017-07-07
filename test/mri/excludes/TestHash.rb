exclude :test_callcc, "needs investigation"
exclude :test_callcc_escape, "needs investigation"
exclude :test_callcc_iter_level, "needs investigation"
exclude :test_callcc_reenter, "needs investigation"
exclude :test_create, "needs investigation"
exclude :test_eql, "needs investigation"
exclude :test_exception_in_rehash, "needs investigation"
exclude :test_fetch_error, "needs investigation"
exclude :test_hash_bignum_hash, "needs investigation"
exclude :test_hash2, "[ruby-core:58993] [Bug #9231]"
exclude :test_inverse_hash, "needs investigation"
exclude :test_NEWHASH_fstring_key, "needs investigation"
exclude :test_recursive_hash_value_struct, "needs investigation"
exclude :test_reject, "needs investigation"
exclude :test_AREF_fstring_key, "Depends on MRI-specific GC.stat key"
exclude :test_ASET_fstring_key, "due https://github.com/jruby/jruby/commit/f3f0091da7d98c5df285"

# These are all excluded as a group because we do not generally randomize hashes.
# We may want or need to follow MRI lead here if we are concerned about the other hashDOS vectors.
# See https://bugs.ruby-lang.org/issues/13002
exclude :test_float_hash_random, "JRuby does not randomize hash calculation for Hash"
exclude :test_integer_hash_random, "JRuby does not randomize hash calculation for Hash"
exclude :test_symbol_hash_random, "JRuby does not randomize hash calculation for Hash"
