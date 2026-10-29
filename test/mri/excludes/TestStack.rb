exclude :test_machine_stack_size, "uses several internal CRuby features for stack size, passes in the interpreter on Linux", run: false
exclude :test_relative_stack_sizes, "uses several internal CRuby features for stack size"
exclude :test_vm_stack_size, "uses several internal CRuby features for stack size, result depends on the platform and mode", run: false
