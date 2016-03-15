windows = RbConfig::CONFIG['host_os'] =~ /mswin|mingw/

exclude :test_signal, "needs investigation" if windows
exclude :test_signal2, "needs investigation"
exclude :test_signal_exception, "needs investigation"
exclude :test_signal_requiring, "needs investigation"
exclude :test_trap, "needs investigation"
