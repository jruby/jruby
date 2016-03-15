windows = RbConfig::CONFIG['host_os'] =~ /mswin|mingw/

# under openssl/test_integration.rb (not name-spaced as OpenSSL)
exclude :test_ca_path_name, "needs investigation" if windows
exclude :test_cipher_strings, "needs investigation" if windows
exclude :test_pathlen_does_not_appear, 'does https://www.paypal.com/ assumptions with ca_file'
exclude :test_ssl_verify, "needs investigation" if windows
