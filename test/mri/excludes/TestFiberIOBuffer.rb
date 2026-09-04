exclude :test_read_write_blocking, "hangs on macos m1"
exclude :test_timeout_after, "hangs on macos m1 probably because timeout is not implemented"
exclude :test_io_buffer_pread_pwrite, "uses Tempfile but does not require the library"
