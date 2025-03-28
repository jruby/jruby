exclude :test_ancestors, "work in progress"
exclude :test_import_methods, "work in progress"
exclude :test_prepend_into_refinement, "zsuper in module issue #5585"
exclude :test_prepend_refined_module, "work in progress"
exclude :test_public_send_should_use_refinements, "not implemented, jruby/jruby#6161"
exclude :test_refine_frozen_class, "work in progress"
exclude :test_refine_prepended_class, "markers not being applied properly to prepended module"
exclude :test_refine_recursion, "work in progress"
exclude :test_refine_scoping, "work in progress"
exclude :test_refined_method_alias_warning, "work in progress"
exclude :test_respond_to_should_use_refinements, "not implemented, jruby/jruby#6161"
exclude :test_super_to_module, "issues finding proper super hierarchy, perhaps related to #5585"
exclude :test_to_proc, "not implemented, jruby/jruby#6161"
exclude :test_tostring, "tainting is no longer supported"
exclude :test_unbound_refine_method, "work in progress"
exclude :test_undef_original_method, "flaw in how we represent undef methods; they look the same as never-defined"
exclude :test_undef_prepended_method, "flaw in how we represent undef methods; they look the same as never-defined"
