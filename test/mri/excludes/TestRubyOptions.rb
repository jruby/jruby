exclude :test_assignment_in_conditional, "line numbers don't match MRI"
exclude :test_backtrace_limit, "work in progress"
exclude :test_chdir, "needs investigation"
exclude :test_copyright, "needs investigation"
exclude :test_crash_report, "fails in JIT mode on Linux CI on GHA"
exclude :test_crash_report_executable_path, "fails in JIT mode on Linux CI on GHA"
exclude :test_crash_report_pipe, "fails in JIT mode on Linux CI on GHA"
exclude :test_crash_report_script, "fails in JIT mode on Linux CI on GHA"
exclude :test_crash_report_script_path, "fails in JIT mode on Linux CI on GHA"
exclude :test_cwd_encoding, "may be intermittent, fails in JIT mode on Linux CI on GHA"
exclude :test_debug, "needs investigation"
exclude :test_dump_insns_with_rflag, "MRI-specific format"
exclude :test_dump_parsetree_error_tolerant, "MRI-specific format"
exclude :test_dump_parsetree_with_rflag, "MRI-specific format"
exclude :test_dump_syntax_with_rflag, "MRI-specific format"
exclude :test_dump_yydebug_with_rflag, "MRI-specific format"
exclude :test_encoding, "needs investigation"
exclude :test_eval, "needs investigation"
exclude :test_flag_in_shebang, "unreliable with our bash script"
exclude :test_free_at_exit_env_var, "needs investigation"
exclude :test_frozen_string_literal, "work in progress"
exclude :test_frozen_string_literal_debug, "needs investigation #4303"
exclude :test_frozen_string_literal_debug_chilled_strings, "different output from warning"
exclude :test_indentation_check, "hangs on macos m1"
exclude :test_invalid_option, "needs investigation"
exclude :test_notfound, "needs investigation"
exclude :test_parser_flag, "needs investigation"
exclude :test_program_name, "needs investigation"
exclude :test_require, "needs investigation"
exclude :test_rubyopt, "needs investigation"
exclude :test_script_from_stdin, "uses io/console in ways we don't support yet"
exclude :test_script_is_directory, "uses io/console in ways we don't support yet"
exclude :test_search, "passes independently but fails in suite"
exclude :test_segv_loaded_features, "fails in JIT mode on Linux CI on GHA"
exclude :test_segv_setproctitle, "fails in JIT mode on Linux CI on GHA"
exclude :test_segv_test, "fails in JIT mode on Linux CI on GHA"
exclude :test_separator, "needs investigation"
exclude :test_sflag, "needs investigation"
exclude :test_shebang, "needs investigation"
exclude :test_syntax_check, "needs investigation"
exclude :test_unmatching_glob, "needs investigation"
exclude :test_unused_variable, "needs investigation"
exclude :test_usage, "expects a specific number of lines in -h output"
exclude :test_usage_long, "needs investigation"
exclude :test_verbose, "work in progress"
exclude :test_version, "work in progress"
exclude :test_warning, "needs investigation"
exclude :test_yydebug, "needs investigation"
