# frozen_string_literal: false
exclude :test_getpass, 'not portable'
exclude :test_oflush, 'needs investigation'
exclude :test_raw_minchar, 'depends on unimplemented PTY functionality'
exclude :test_raw_timeout, 'depends on unimplemented PTY functionality'
exclude :test_set_winsize_invalid_dev, 'depends on unimplemented PTY functionality'
