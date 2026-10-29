exclude :test_system, "needs investigation"
exclude :test_system_closed, "uses ObjectSpace to check if IO gets closed"
exclude :test_system_exception_nonascii, "on Linux, a script without a shebang exits 0 instead of running with sh" if RbConfig::CONFIG["host_os"] =~ /linux/
