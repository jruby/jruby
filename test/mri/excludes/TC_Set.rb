exclude :test_compare_by_identity_compact, "no GC.verify_compaction_references method"
exclude :test_compare_by_identity_preservation, "Set operations do not preserve compare_by_identity yet"
exclude :test_iteration_mutation_guards, "Set does not raise when mutated during iteration yet"
exclude :test_xor, "Set#^ does not preserve compare_by_identity yet"
