exclude :test_authenticated_proxy_http_basic_authentication_success, "hangs on Linux or very slow", run: false
exclude :test_proxy, "hangs or very slow on Linux", run: false
exclude :test_proxy_http_basic_authentication_failure, "hangs or very slow on Linux", run: false
exclude :test_proxy_http_basic_authentication_success, "hangs or very slow on MacOS", run: false
