windows = RbConfig::CONFIG['host_os'] =~ /mswin|mingw/

exclude :test_accept_nonblock, "needs investigation"
exclude :test_accept_nonblock_error, "needs investigation"
exclude :test_connect_nonblock, "needs investigation"
exclude :test_recv_nonblock_error, "need investigation" if windows
exclude :test_recvmsg_nonblock_error, "needs investigation"
exclude :test_sendmsg_nonblock_error, "needs investigation"
exclude :test_socket_recvfrom_nonblock, "needs investigation"
exclude :test_udp_recv_nonblock, "needs investigation"
exclude :test_udp_recvfrom_nonblock, "needs investigation"
