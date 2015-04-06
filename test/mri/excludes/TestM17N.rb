exclude :test_nonascii_method_name, "lexer is not pulling mbc characters off the wire correctly"
exclude :test_symbol, "management of differently-encoded symbols is not right"
exclude :test_symbol_op, "some symbols are created early and do not have UTF-8 encoding; management of differently-encoded symbols is not right"
