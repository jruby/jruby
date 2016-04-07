windows = RbConfig::CONFIG['host_os'] =~ /mswin|mingw/

exclude :test_LSHIFT, "needs investigation"
exclude :test_dummy_inspect, "needs investigation"
exclude :test_gsub_enumerator, "needs investigation"
exclude :test_hash_random, "needs investigation"
exclude :test_partition, "needs investigation"
exclude :test_hash_random, "JRuby #3275" if !windows
exclude :test_rpartition, "needs investigation"
exclude :test_rstrip, "needs investigation"
exclude :test_split, "needs investigation"
