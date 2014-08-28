fails:IO#dup returns a new IO instance
fails:IO#dup sets a new descriptor on the returned object
fails:IO#dup allows closing the new IO without affecting the original
fails:IO#dup allows closing the original IO without affecting the new one
fails:IO#dup raises IOError on closed stream
