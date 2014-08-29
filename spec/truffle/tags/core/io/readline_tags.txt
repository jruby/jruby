fails:IO#readline returns the next line on the stream
fails:IO#readline goes back to first position after a rewind
fails:IO#readline returns characters after the position set by #seek
fails:IO#readline raises EOFError on end of stream
fails:IO#readline raises IOError on closed stream
fails:IO#readline assigns the returned line to $_
