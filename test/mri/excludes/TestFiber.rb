exclude :test_fatal_in_fiber, "needs CRuby's -test-/fatal extension, which we do not ship"
exclude :test_no_valid_cfp, "raises NPE rather than detecting bad context and raising a Ruby error"
exclude :test_separate_lastmatch, "$~ is shared with the parent fiber rather than per-fiber"
