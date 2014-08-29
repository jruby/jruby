fails:IO#lineno raises an IOError on a closed stream
fails:IO#lineno returns the current line number
fails:IO#lineno= raises an IOError on a closed stream
fails:IO#lineno= calls #to_int on a non-numeric argument
fails:IO#lineno= truncates a Float argument
fails:IO#lineno= raises TypeError on nil argument
fails:IO#lineno= sets the current line number to the given value
fails:IO#lineno= does not change $.
fails:IO#lineno= does not change $. until next read
