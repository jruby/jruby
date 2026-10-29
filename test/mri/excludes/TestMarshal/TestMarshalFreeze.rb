exclude :test_extend_string, "attempts to instance_eval a frozen String, which breaks because we create singleton class (https://github.com/jruby/jruby/issues/8638)"
exclude :test_object_prepend, "work in progress"
exclude :test_singleton, "work in progress"
exclude :test_string_ivar, "work in progress, may only fail in JIT", run: false
exclude :test_string_subclass, "attempts to instance_eval a frozen String, which breaks because we create singleton class (https://github.com/jruby/jruby/issues/8638)"
exclude :test_string_subclass_cycle, "attempts to instance_eval a frozen String, which breaks because we create singleton class (https://github.com/jruby/jruby/issues/8638)"
exclude :test_string_subclass_extend, "attempts to instance_eval a frozen String, which breaks because we create singleton class (https://github.com/jruby/jruby/issues/8638)"
