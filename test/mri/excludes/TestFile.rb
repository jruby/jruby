exclude :test_open_tempfile_path, "fails in JIT mode on Linux CI on GHA", run: false
exclude :test_stat, "requires CRuby internals test extension, passes on Linux", run: false