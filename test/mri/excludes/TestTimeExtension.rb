exclude :test_iso8601_encode, "works except that we only have nano-sec precision up to 6-decimal values"
exclude :test_strptime_s_N, "1/1000000000000 subsec precision not supported"
exclude :test_xmlschema_encode, "works except that we only have nano-sec precision up to 6-decimal values"
