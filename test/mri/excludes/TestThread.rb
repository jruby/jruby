# frozen_string_literal: false
exclude(/_stack_size$/, 'often too expensive')
exclude :test_mutex_interrupt, "hangs"
exclude :test_thread_join_main_thread, "hangs"