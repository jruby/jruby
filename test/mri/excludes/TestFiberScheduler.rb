exclude :test_condition_variable, "hangs on macos M1"
exclude :test_current_scheduler, "fails intermittently on Linux CI on GHA"
exclude :test_iseq_compile_under_gc_stress_bug_21180, "uses RubyVM"
