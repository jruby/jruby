exclude :test_chdir, "uses user.home as a fallback home path"
exclude :test_dir_enc, "second argument (hash with encoding) for Dir.open is not supported"
exclude :test_glob, '\0 delimiter is not handled'
exclude :test_glob_cases, "fails to return files with their correct casing (#2150)"
exclude :test_glob_gc_for_fd, "tweaks rlimit and never restores it, depends on GC effects"
exclude :test_home, "uses user.home as a fallback home path"
exclude :test_unknown_keywords, "https://github.com/jruby/jruby/issues/1368"
