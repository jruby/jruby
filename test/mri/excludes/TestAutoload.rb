exclude :test_autoload_while_autoloading, "needs investigation"
exclude :test_bug_13526, "racey test that doesn't behave as expected with concurrent threads #5294"
exclude :test_require_implemented_in_ruby_is_called, "attempted to dispatch to require but it seems to recursively call itself"
