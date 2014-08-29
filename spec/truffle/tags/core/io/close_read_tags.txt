fails:IO#close_read closes the read end of a duplex I/O stream
fails:IO#close_read raises an IOError on subsequent invocations
fails:IO#close_read allows subsequent invocation of close
fails:IO#close_read raises an IOError if the stream is writable and not duplexed
fails:IO#close_read closes the stream if it is neither writable nor duplexed
fails:IO#close_read raises IOError on closed stream
