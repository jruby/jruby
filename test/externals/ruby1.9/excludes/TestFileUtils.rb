windows = RbConfig::CONFIG['host_os'] =~ /mswin|mingw/

exclude :test_chmod, "needs investigation" if windows
exclude :test_chmod_R, "needs investigation" if windows
exclude :test_chmod_symbol_mode, "needs investigation" if windows
exclude :test_chmod_symbol_mode_R, "needs investigation" if windows
exclude :test_copy_entry_symlink, "needs investigation"
exclude :test_cp_symlink, "needs investigation" if windows
exclude :test_cp_r_symlink, "needs investigation"
exclude :test_install, "needs investigation" if windows
exclude :test_install_symlink, "needs investigation" if windows
exclude :test_ln_s, "needs investigation"
exclude :test_ln_sf, "needs investigation"
exclude :test_ln_symlink, "needs investigation" if windows
exclude :test_mkdir, "needs investigation" if windows
exclude :test_mkdir_file_perm, "needs investigation" if windows
exclude :test_mkdir_p, "needs investigation" if windows
exclude :test_mkdir_p_file_perm, "needs investigation" if windows
exclude :test_mv_symlink, "needs investigation" if windows
exclude :test_pwd, "needs investigation" if windows
exclude :test_remove_entry_secure_symlink, "needs investigation" if windows
exclude :test_rm_symlink, "needs investigation" if windows
exclude :test_rm_r_symlink, "needs investigation" if windows
