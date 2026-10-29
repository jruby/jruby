exclude :test_current_scheduler, "fails intermittently on Linux CI on GHA"
exclude :test_iseq_compile_under_gc_stress_bug_21180, "uses RubyVM"
exclude :test_io_wait_preserves_errno, "times out when io_wait returns nil or 0"
