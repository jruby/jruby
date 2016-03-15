windows = RbConfig::CONFIG['host_os'] =~ /mswin|mingw/

exclude :test_del_trailing_separator_162, "needs investigation" if windows
exclude :test_del_trailing_separator_187, "needs investigation" if windows
exclude :test_each_line, "needs investigation" if windows
exclude :test_executable?, "needs investigation" if windows
exclude :test_lchown, "needs investigation" if windows
exclude :test_lstat, "needs investigation" if windows
exclude :test_make_symlink, "needs investigation" if windows
exclude :test_open, "needs investigation"
exclude :test_readlink, "needs investigation" if windows
exclude :test_realdirpath, "needs investigation"
exclude :test_realpath, "needs investigation"
exclude :test_lchmod, "fails on Travis, maybe on Linux in general?"
