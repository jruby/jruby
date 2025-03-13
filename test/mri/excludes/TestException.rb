# frozen_string_literal: false
reason = %[\
Because machine stack overflow can happen anywhere, even critical
sections including external libraries, it is very neary impossible to
recover from such situation.
]

exclude %r[test_machine_stackoverflow], reason

exclude :test_warning_warn_super, "GC is not configurable in JRuby"