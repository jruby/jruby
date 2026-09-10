exclude :test_wait_mask_readable, "test_io_wait_uncommon.rb shares the run and requires 'io/wait', whose jar redefines IO#wait with the pre-10.0 implementation"
exclude :test_wait_mask_writable, "test_io_wait_uncommon.rb shares the run and requires 'io/wait', whose jar redefines IO#wait with the pre-10.0 implementation"
