# frozen_string_literal: false
exclude :test_noctty, 'we return an IO.console even when not a TTY'
exclude :test_cooked, "needs investigation"
exclude :test_getch_timeout, "needs investigation"
exclude :test_raw_minchar, "needs investigation"
exclude :test_raw_timeout, "needs investigation"
exclude :test_raw, "needs investigation"