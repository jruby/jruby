exclude :test_read_write_blocking, "blocks the fiber's thread until we support the blocking_operation_wait hook"
exclude :test_io_buffer_pread_pwrite, "uses Tempfile but does not require the library"
