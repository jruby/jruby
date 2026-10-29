exclude :test_abort, "work in progress"
exclude :test_argv0, "needs investigation"
exclude :test_argv0_noarg, "hangs", run: false
exclude :test_aspawn_too_long_path, "unsupported option rlimit_nproc"
exclude :test_clock_getres_CLOCK_BASED_CLOCK_PROCESS_CPUTIME_ID, "missing process timing functionality"
exclude :test_clock_getres_TIME_BASED_CLOCK_REALTIME, "missing process timing functionality"
exclude :test_clock_gettime_CLOCK_BASED_CLOCK_PROCESS_CPUTIME_ID, "missing process timing functionality"
exclude :test_clock_gettime_TIME_BASED_CLOCK_REALTIME, "missing process timing functionality"
exclude :test_exec_wordsplit, "fails on Linux at least", run: false
exclude :test_execopts_chdir, "needs investigation"
exclude :test_execopts_close_others, "ENOENT looking for foo"
exclude :test_execopts_env, "needs investigation, writes files into the working directory when run", run: false
exclude :test_execopts_env_single_word, "needs investigation"
exclude :test_execopts_exec, "ENOENT looking for foo, writes files into the working directory when run", run: false
exclude :test_execopts_open_chdir, "needs investigation, writes files into the working directory when run", run: false
exclude :test_execopts_open_chdir_m17n_path, "needs investigation #4303, writes files into the working directory when run", run: false
exclude :test_execopts_open_failure, "may depend on error handling after fork, not possible with posix_spawn"
exclude :test_execopts_pgroup, "ESPIPE illegal seek on bin/jruby"
exclude :test_execopts_popen, "needs investigation"
exclude :test_execopts_popen_extra_fd, "appears chdir is screwing up the resulting command line, writes files into the working directory when run", run: false
exclude :test_execopts_redirect_dup2_child, "Errno::ENOENT: No such file or directory - out, writes files into the working directory when run", run: false
exclude :test_execopts_redirect_fd, "incorrect output from subprocess?, writes files into the working directory when run", run: false
exclude :test_execopts_redirect_nonascii_path, "needs investigation, writes files into the working directory when run", run: false
exclude :test_execopts_redirect_open_fifo, "fails on Linux at least", run: false
exclude :test_execopts_redirect_open_fifo_interrupt_raise, "hangs on MacOS", run: false
exclude :test_execopts_redirect_open_order_normal, "requires Kernel.system to support chdir and options, writes files into the working directory when run", run: false
exclude :test_execopts_redirect_open_order_reverse, "requires Kernel.system to support chdir and options, writes files into the working directory when run", run: false
exclude :test_execopts_redirect_pipe, "hangs", run: false
exclude :test_execopts_redirect_symbol, "Errno::ENOENT: No such file or directory - out, writes files into the working directory when run", run: false
exclude :test_execopts_redirect_to_out_and_err, "Errno::ENOENT: No such file or directory - foo, writes files into the working directory when run", run: false
exclude :test_execopts_rlimit, "posix_spawn does not support rlimit modification"
exclude :test_execopts_umask, "unsupported"
exclude :test_execopts_unsetenv_others, "unsupported"
exclude :test_gid_re_exchangeable_p, "unimplemented"
exclude :test_gid_sid_available?, "unimplemented"
exclude :test_initgroups, "unimplemented"
exclude :test_popen_wordsplit, "needs investigation"
exclude :test_popen_wordsplit_beginning_and_trailing_spaces, "needs investigation"
exclude :test_spawn_too_long_path, "wrong exec option symbol: rlimit_nproc"
exclude :test_status_quit, "needs investigation"
exclude :test_status_wait, "work in progress"
exclude :test_to_hash_on_arguments, "needs investigation"
exclude :test_uid_re_exchangeable_p, "unimplemented"
exclude :test_uid_sid_available?, "unimplemented"
exclude :test_wait_exception, "causes spurious interrupts due to racey thread state checks", run: false
exclude :test_warmup_frees_pages, "depends on CRuby memory dumping API"
exclude :test_warmup_precompute_string_coderange, "depends on CRuby memory dumping API"
exclude :test_warmup_promote_all_objects_to_oldgen, "depends on CRuby memory dumping API"
exclude :test_warmup_run_major_gc_and_compact, "depends on CRuby memory dumping API"
