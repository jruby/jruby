exclude :test_bind_to_local_host, 'started hanging (but only) on Travis CI' if ENV['CI']
exclude :test_bind_to_local_port, 'started hanging (but only) on Travis CI' if ENV['CI']