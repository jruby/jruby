exclude :test_connect_accept_nonblock_no_exception, "SSLError: Cannot change mode after SSL traffic has started"
exclude :test_gets_chomp, "needs investigation"
exclude :test_readbyte, "undefined method 'readbyte' for an instance of OpenSSL::SSL::SSLSocket"
exclude :test_readbyte_eof, "undefined method 'readbyte' for an instance of OpenSSL::SSL::SSLSocket"
