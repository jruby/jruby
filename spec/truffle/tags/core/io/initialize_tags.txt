fails:IO#initialize reassociates the IO instance with the new descriptor when passed a Fixnum
fails:IO#initialize calls #to_int to coerce the object passed as an fd
fails:IO#initialize raises a TypeError when passed an IO
fails:IO#initialize raises a TypeError when passed nil
fails:IO#initialize raises a TypeError when passed a String
fails:IO#initialize raises IOError on closed stream
fails:IO#initialize raises an Errno::EBADF when given an invalid file descriptor
