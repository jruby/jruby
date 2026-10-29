exclude :test_initialize_failure, "needs investigation"
exclude :test_initialize_resolv_timeout_with_connection_failure, "needs investigation"
exclude :test_initialize_v6_hostname_resolved_earlier_and_v6_server_is_not_listening, "needs investigation"
exclude :test_initialize_v6_hostname_resolved_later_and_v6_server_is_not_listening, "needs investigation"
exclude :test_inspect, "missing socket information"
exclude :test_recvfrom, "no addrinfo returned"
exclude :test_tcp_initialize_open_timeout, "connection refused to localhost", run: false
