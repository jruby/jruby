exclude :test_io_close_blocking_thread, "on Linux, a thread blocked reading the IO sees EOF or stays blocked instead of raising IOError when it is closed"
