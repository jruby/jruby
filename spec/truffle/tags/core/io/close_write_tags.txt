fails:IO#close_write closes the write end of a duplex I/O stream
fails:IO#close_write raises an IOError on subsequent invocations
fails:IO#close_write allows subsequent invocation of close
fails:IO#close_write raises an IOError if the stream is readable and not duplexed
fails:IO#close_write closes the stream if it is neither readable nor duplexed
fails:IO#close_write flushes and closes the write stream
fails:IO#close_write raises IOError on closed stream
