exclude :test_condition_variable, "hangs on macos m1"
exclude :test_mutex_deadlock, "subprocess times out probably not raising deadlock error"
exclude :test_mutex_fiber_raise, "hangs on macos m1"
exclude :test_queue_pop_waits, "wonky subprocess launching in test"
