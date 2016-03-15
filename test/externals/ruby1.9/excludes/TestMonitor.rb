windows = RbConfig::CONFIG['host_os'] =~ /mswin|mingw/

exclude :test_killed_thread_in_synchronize, 'needs investigation' if windows
