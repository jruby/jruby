exclude :test_AREF_fstring_key, "depends on ObjectSpace.count_objects"
exclude :test_AREF_fstring_key_default_proc, "needs investigation"
exclude :test_any_hash_fixable, "launches many subprocesses and does not test anything useful on JRuby"
exclude :test_compare_by_id_memory_leak, "no working assert_no_memory_leak method"
exclude :test_compare_by_identy_memory_leak, "no working assert_no_memory_leak method"
exclude :test_exception_in_rehash_memory_leak, "no working assert_no_memory_leak method"
exclude :test_fetch_error, "needs investigation"
exclude :test_inspect, "not matching special symbols requiring hashrocket vs colon"
exclude :test_integer_hash_random, "JRuby does not randomize hash calculation for integer keys, see https://bugs.ruby-lang.org/issues/13002"
exclude :test_memory_size_after_delete, "uses ObjectSpace.memsize_of"
exclude :test_rehash_memory_leak, "no working assert_no_memory_leak method"
exclude :test_replace_memory_leak, "no working assert_no_memory_leak method"
exclude :test_st_literal_memory_leak, "no working assert_no_memory_leak method"
