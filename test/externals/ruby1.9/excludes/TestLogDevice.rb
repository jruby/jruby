windows = RbConfig::CONFIG['host_os'] =~ /mswin|mingw/

exclude :test_write, "needs investigation" if windows
