exclude :test_default_seed, "error in assert_separately logic in test/unit"
exclude :test_random_float, "likely fixed by MRI r24670"
exclude :test_random_ulong_limited_no_rand, "Formatter moved from SecureRandom to Random before 2.4, modified in 2.4 (#4687)"
