# frozen_string_literal: false
exclude :test_noctty, 'we return an IO.console even when not a TTY'
exclude :test_getpass, 'our PTY library does not set a controlling terminal'
