windows = RbConfig::CONFIG['host_os'] =~ /mswin|mingw/

exclude :test_split_content, '\r\n in heredoc from git' if windows
exclude :test_graceful_parsing_failure, 'needs investigation'
