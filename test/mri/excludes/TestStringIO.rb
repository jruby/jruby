exclude :test_overflow, "unusual subprocess test trying to overflow some value"
exclude :test_write_integer_overflow, "JVM does not support > 32bit signed array offsets, so our StringIO cannot either"
