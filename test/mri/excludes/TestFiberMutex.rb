exclude :test_queue, "Thread::Queue#pop does not defer to Fiber::Scheduler, so it blocks the thread that has to run the scheduler"
exclude :test_queue_pop_waits, "wonky subprocess launching in test"
