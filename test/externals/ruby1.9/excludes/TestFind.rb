windows = RbConfig::CONFIG['host_os'] =~ /mswin|mingw/

exclude :test_change_dir_to_file, "needs investigation" if windows
exclude :test_unreadable_dir, "needs investigation" if windows
exclude :test_unsearchable_dir, "fails on travis"
