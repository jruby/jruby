windows = RbConfig::CONFIG['host_os'] =~ /mswin|mingw/

exclude :test_basename, "needs investigation"
exclude :test_dirname, "needs investigation"
exclude :test_chmod, "needs investigation" if windows
exclude :test_directory_p, "needs investigation" if windows
exclude :test_executable_p, "needs investigation" if windows
exclude :test_executable_real_p, "needs investigation" if windows
exclude :test_expand_path, "needs investigation"
exclude :test_expand_path_cleanup_dots_file_name, "needs investigation" if windows
exclude :test_expand_path_converts_a_dot_with_unc_dir, "needs investigation" if windows
exclude :test_expand_path_converts_a_pathname_which_starts_with_a_slash_and_unc_pathname, "needs investigation" if windows
exclude :test_expand_path_converts_a_pathname_which_starts_with_a_slash_using_host_share, "needs investigation" if windows
exclude :test_expand_path_preserves_unc_path_root, "needs investigation" if windows
exclude :test_expand_path_remove_trailing_alternative_data, "needs investigation" if windows
exclude :test_expand_path_removes_trailing_spaces_from_absolute_path, "needs investigation" if windows
exclude :test_extname, "needs investigation"
exclude :test_find_file, "needs investigation"
exclude :test_join, "needs investigation" if windows
exclude :test_owned_p, "needs investigation" if windows
exclude :test_stat, "needs investigation"
exclude :test_stat_init, "needs investigation"
exclude :test_stat_executable_p, "needs investigation" if windows
exclude :test_stat_executable_real_p, "needs investigation" if windows
exclude :test_stat_owned_p, "needs investigation" if windows
exclude :test_stat_world_readable_p, "needs investigation" if windows
exclude :test_stat_world_writable_p, "needs investigation" if windows
exclude :test_umask, "needs investigation"
exclude :test_unlink, "needs investigation"
exclude :test_utime, "needs investigation"
exclude :test_world_readable_p, "needs investigation"
exclude :test_world_writable_p, "needs investigation"
exclude :test_writable_p, "needs investigation"
exclude :test_writable_real_p, "needs investigation"
exclude :test_zero_p, "needs investigation"
exclude :test_lchmod, "Fails on Linux"
exclude :test_expand_path_home, "needs investigation"
exclude :test_expand_path_returns_tainted_strings_or_not, "needs investigation"
