fails:IO#write_nonblock on a file writes all of the string's bytes but does not buffer them
fails:IO#write_nonblock on a file checks if the file is writable if writing zero bytes
fails:IO#write_nonblock coerces the argument to a string using to_s
fails:IO#write_nonblock checks if the file is writable if writing more than zero bytes
fails:IO#write_nonblock returns the number of bytes written
fails:IO#write_nonblock invokes to_s on non-String argument
fails:IO#write_nonblock writes all of the string's bytes without buffering if mode is sync
fails:IO#write_nonblock does not warn if called after IO#read
fails:IO#write_nonblock writes to the current position after IO#read
fails:IO#write_nonblock advances the file position by the count of given bytes
fails:IO#write_nonblock raises IOError on closed stream
