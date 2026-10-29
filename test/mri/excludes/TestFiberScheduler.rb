exclude :test_autoload, "autoload holds a Java monitor across Fiber switches, so a sibling fiber hangs instead of deferring to the scheduler"
exclude :test_iseq_compile_under_gc_stress_bug_21180, "uses RubyVM"
exclude :test_io_wait_preserves_errno, "times out when io_wait returns nil or 0"
