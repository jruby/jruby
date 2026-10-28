exclude :test_compare_by_identity_compact, "GC.verify_compaction_references is not supported"
exclude :test_compare_by_identity_preservation, "compare_by_identity is not preserved by derived sets"
exclude :test_iteration_mutation_guards, "Set does not raise when mutated during iteration"
exclude :test_xor, "Set#^ does not use the receiver's comparison for the result"
