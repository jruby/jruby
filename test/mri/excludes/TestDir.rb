exclude :test_chdir_conflict, "work in progress"
exclude :test_glob, '\0 delimiter is not handled'
exclude :test_glob_gc_for_fd, "tweaks rlimit and never restores it, depends on GC effects"
exclude :test_glob_ignore_casefold_invalid_encoding, "work in progress"
exclude :test_glob_too_may_open_files, "our glob does not appear to open files, and so the expected EMFILE does not happen"
