fails:TCPSocket partial closability if the write end is closed then the other side can read past EOF without blocking
fails:TCPSocket partial closability closing the write end ensures that the other side can read until EOF
