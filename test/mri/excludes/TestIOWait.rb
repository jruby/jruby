exclude :test_wait_eof, "JDK select appears to produce ready for read when pipe closes?!"
exclude :test_wait_writable_EPIPE, "hangs, maybe due to write buffering?"
