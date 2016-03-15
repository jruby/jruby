windows = RbConfig::CONFIG['host_os'] =~ /mswin|mingw/

exclude :test_table, "needs investigation" if windows
