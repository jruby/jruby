exclude :test_dump, "tests MRI-specific exception"
exclude :test_queue_close_multi_multi, "flakey on Travis: no threads runnning"
exclude :test_queue_with_trap, "hangs?"
