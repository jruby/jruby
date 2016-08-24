windows = RbConfig::CONFIG['host_os'] =~ /mswin|mingw/

exclude :test_capture2, "hangs"
exclude :test_capture2e, "error"
exclude :test_capture3, "needs investigation" if windows
exclude :test_capture3_flip, "needs investigation" if windows
exclude :test_open3, "wtf weirdly broke with no explanation" if windows
exclude :test_pid, "needs investigation" if windows
exclude :test_pipeline, "error"
exclude :test_pipeline_r, "hangs"
exclude :test_pipeline_rw, "hangs"
exclude :test_pipeline_start, "error"
exclude :test_pipeline_start_noblock, "error"
exclude :test_pipeline_w, "hangs"
exclude :test_popen2, "hangs"
exclude :test_popen2e, "error"
exclude :test_stderr, "needs investigation" if windows
exclude :test_stdin, "error"
exclude :test_stdout, "needs investigation" if windows
