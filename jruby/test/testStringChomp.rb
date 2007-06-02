require 'test/minirunit'

test_check "Test String#chomp compatibility with ruby"

LF = "\C-j"
CR = "\C-m"

# puts($/[0])     # ruby and jruby both show 10 under windows (surpising? as windows is CRLF and unix LF)
# puts($/[1])     # ruby and jruby both show nill under windows

########################################
# Special case parameter is empty (default to $/)
# One and only one line ending is removed
########################################
# Standard platfom line endings
test_ok(("CRLF" + CR + LF).chomp == "CRLF") # windows
test_ok(("LF" + LF).chomp == "LF") # unix
test_ok(("CR" + CR).chomp == "CR") # mac
test_ok(("LFCR" + LF + CR).chomp == "LFCR" + LF) # HAL9000

# Corner cases
test_ok((CR + LF).chomp == "") # windows
test_ok((LF).chomp == "") # unix
test_ok((CR).chomp == "") # mac
test_ok((LF + CR).chomp == LF) # HAL9000

# Standard platfom double line endings (only one occurence is removed)
test_ok(("CRLF" + CR + LF + CR + LF).chomp == "CRLF" + CR + LF) # windows
test_ok(("LF" + LF + LF).chomp == "LF" + LF) # unix
test_ok(("CR" + CR + CR).chomp == "CR" + CR) # mac

# Extra LFs after normal LF
test_ok(("CRLF" + CR + LF + LF).chomp == "CRLF" + CR + LF) # windows plus one extra LF
test_ok(("LF" + LF + LF).chomp == "LF" + LF)  # unix plus one extra LF

test_ok("No line ending".chomp == "No line ending")  


########################################
# Special case parameter "\n" treated identically to no parameter
# One and only one line ending is removed
########################################
# Standard platfom line endings
test_ok(("CRLF" + CR + LF).chomp("\n") == "CRLF") # windows
test_ok(("LF" + LF).chomp("\n") == "LF") # unix
test_ok(("CR" + CR).chomp("\n") == "CR") # mac
test_ok(("LFCR" + LF + CR).chomp("\n") == "LFCR" + LF) # HAL9000

# Corner cases
test_ok((CR + LF).chomp("\n") == "") # windows
test_ok((LF).chomp("\n") == "") # unix
test_ok((CR).chomp("\n") == "") # mac
test_ok((LF + CR).chomp("\n") == LF) # HAL9000

# Standard platfom double line endings (only one occurence is removed)
test_ok(("CRLF" + CR + LF + CR + LF).chomp("\n") == "CRLF" + CR + LF) # windows
test_ok(("LF" + LF + LF).chomp("\n") == "LF" + LF) # unix
test_ok(("CR" + CR + CR).chomp("\n") == "CR" + CR) # mac

# Extra LFs after normal LF
test_ok(("CRLF" + CR + LF + LF).chomp("\n") == "CRLF" + CR + LF) # windows plus one extra LF
test_ok(("LF" + LF + LF).chomp("\n") == "LF" + LF)  # unix plus one extra LF

test_ok("No line ending".chomp("\n") == "No line ending")  


########################################
# Special case parameter is ""
# Multiple unix and windows line ending removed but mac line endings left unchanged
########################################
# Standard platfom line endings
test_ok(("CRLF" + CR + LF).chomp("") == "CRLF") # windows
test_ok(("LF" + LF).chomp("") == "LF") # unix
test_ok(("CR" + CR).chomp("") == "CR" + CR) # mac No CR removed
test_ok(("LFCR" + LF + CR).chomp("") == "LFCR" + LF + CR) # HAL9000 No CR removed

# Corner cases
test_ok((CR + LF).chomp("") == "") # windows
test_ok((LF).chomp("") == "") # unix
test_ok((CR).chomp("") == CR) # mac
test_ok((LF + CR).chomp("") == LF + CR) # HAL9000

# Standard platfom double line endings (only one occurence is removed)
test_ok(("CRLF" + CR + LF + CR + LF).chomp("") == "CRLF") # windows ALL multiple CR LFs removed
test_ok(("LF" + LF + LF).chomp("") == "LF") # unix ALL multiple LFs removed
test_ok(("CR" + CR + CR).chomp("") == "CR" + CR + CR) # mac NO multiple CRs removed

# Extra LFs after normal LF
test_ok(("CRLF" + CR + LF + LF).chomp("") == "CRLF") # windows plus one extra LF
test_ok(("LF" + LF + LF).chomp("") == "LF")  # unix plus one extra LF

test_ok("No line ending".chomp("") == "No line ending")  

########################################
# Other parameters must be an exact match
########################################
test_ok("hello".chomp("llo") == "he")
test_ok(("CR" + CR).chomp(CR) == "CR") # remove fixed number of CRs 
test_ok(("CR" + CR + CR).chomp(CR + CR) == "CR") # remove fixed number of CRs 
test_ok(("CR" + CR).chomp(CR + CR) == "CR" + CR) 
test_ok(("LFCR" + LF + CR).chomp(LF + CR) == "LFCR") # HAL9000

test_ok("No line ending".chomp("llo") == "No line ending")  

