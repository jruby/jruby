exclude :test_replace_wb_variable_width_alloc, "no GC.verify_internal_consistency method"
exclude :test_shared_marking, "no GC.verify_internal_consistency method"
exclude :test_short_heap_array_sort_bang_memory_leak, "no working assert_no_memory_leak method"
exclude :test_slice_gc_compact_stress, "GC is not configurable in JRuby"
