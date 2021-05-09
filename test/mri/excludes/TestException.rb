# frozen_string_literal: false
reason = %[\
Because machine stack overflow can happen anywhere, even critical
sections including external libraries, it is very neary impossible to
recover from such situation.
]

exclude :test_circular_cause_handle, "unfinished in initial 2.6 work, #6161"
exclude :test_errinfo_encoding_in_debug, "parser issue with Japanese encodings (https://github.com/jruby/jruby/issues/3679)"
exclude :test_full_message, "work in process to support highlighting and reverse trace (jruby/jruby#5510)"
exclude /test_machine_stackoverflow/, reason
exclude :test_machine_stackoverflow_by_define_method, reason
exclude :test_multibyte_and_newline, "Exception messages always go through Java String for us"
exclude :test_name_error_info_local_variables, "JRuby does not support extracting local variables from a NameError"
exclude :test_name_error_info_method, "method_missing errors do not have original call type available"
exclude :test_name_error_info_method_missing, "method_missing errors do not have original call type available"
exclude :test_name_error_info_parent_iseq_mark, "JRuby does not support extracting local variables from a NameError"
exclude :test_non_exception_cause, "unfinished in initial 2.6 work, #6161"
exclude :test_output_string_encoding, "Exception messages always go through Java String for us"
exclude :test_redefined_backtrace, "Our backtrace is lazily set up and the flow does not work with this change"
exclude :test_stackoverflow, reason
exclude :test_too_many_args_in_eval, "MRI raises SystemStackError for huge number of args, for some reason"
exclude :test_warning_warn, "we warn a line at a time"
exclude :test_warning_warn_circular_require_backtrace, "we do not support #path objects in loaded features"
exclude :test_wrong_backtrace, "improvements required for full_message to use #backtrace results (jruby/jruby#5510)"
