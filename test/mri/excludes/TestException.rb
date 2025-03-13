# frozen_string_literal: false
reason = %[\
Because machine stack overflow can happen anywhere, even critical
sections including external libraries, it is very neary impossible to
recover from such situation.
]

exclude %r[test_machine_stackoverflow], reason

exclude :test_cause_exception_in_cause_message, "work in progress"
exclude :test_control_in_message, "work in progress"
exclude :test_exception_in_ensure_with_next, "work in progress"
exclude :test_kernel_warn_uplevel, "work in progress"
exclude :test_warn_deprecated_category, "work in progress"
exclude :test_warn_deprecated_to_remove_category, "work in progress"
exclude :test_warning_category_deprecated, "work in progress"
exclude :test_warning_category_experimental, "work in progress"
exclude :test_warning_warn_super, "GC is not configurable"
exclude :test_warning_warn_super, "GC is not configurable"
