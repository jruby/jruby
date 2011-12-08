exclude :test_each_line_limit_0
exclude :test_each_line_paragraph
exclude :test_inplace2
exclude :test_inplace3
exclude :test_inplace_dup
exclude :test_inplace_no_backup
exclude :test_inplace_rename_impossible
exclude :test_inplace_stdin
exclude :test_inplace_stdin2
exclude :test_lineno2
exclude :test_lineno3
exclude :test_readlines_limit_0
exclude :test_readpartial2
exclude :test_unreadable

# These are all excluded because the popen that the test uses
# hangs on JRuby.
exclude :test_argf, "hangs"
exclude :test_argv, "hangs"
exclude :test_binmode, "hangs"
exclude :test_close, "hangs"
exclude :test_closed, "hangs"
exclude :test_each_byte, "hangs"
exclude :test_each_char, "hangs"
exclude :test_each_line, "hangs"
exclude :test_encoding, "hangs"
exclude :test_eof, "hangs"
exclude :test_file, "hangs"
exclude :test_filename, "hangs"
exclude :test_filename2, "hangs"
exclude :test_fileno, "hangs"
exclude :test_getbyte, "hangs"
exclude :test_getc, "hangs"
exclude :test_inplace, "hangs"
exclude :test_lineno, "hangs"
exclude :test_read, "hangs"
exclude :test_read2, "hangs"
exclude :test_read3, "hangs"
exclude :test_readbyte, "hangs"
exclude :test_readchar, "hangs"
exclude :test_readpartial, "hangs"
exclude :test_rewind, "hangs"
exclude :test_seek, "hangs"
exclude :test_set_pos, "hangs"
exclude :test_skip, "hangs"
exclude :test_tell, "hangs"
exclude :test_textmode, "hangs"
exclude :test_to_io, "hangs"
