exclude :test_deflate_stream, "stream support is new in 2.2 (#2128)"
exclude :test_gunzip, "TODO: [Zlib::GzipFile::Error] exception expected"
exclude :test_gunzip_errors, "jzlib errors somewhat differently"
exclude :test_gunzip_no_memory_leak, "no working assert_no_memory_leak method"
