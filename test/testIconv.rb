require 'test/minirunit'
test_check "Iconv:"

require 'iconv'

# FIXME: Not implemented yet (though core work done to support it) [JRUBY-309]
#test_exception(Iconv::IllegalSequence) { p Iconv.conv("ASCII", "UTF-8", "ol\303\251") }

# FIXME: Java does not support transliteration in core jdk?
#Iconv.iconv("ASCII//TRANSLIT", "UTF-8", "ol\303\251")

# JRUBY-867, make us ignore transliteration stuff since we can't support it

result = Iconv.conv("ISO-8859-1//TRANSLIT", "UTF-8", "ol\303\251")
test_equal("ol\351", result)
result = Iconv.conv("ISO-8859-1//IGNORE", "UTF-8", "ol\303\251")
test_equal("ol\351", result)
result = Iconv.conv("ISO-8859-1//IGNORE//TRANSLIT", "UTF-8", "ol\303\251")
test_equal("ol\351", result)

result = Iconv.conv("ISO-8859-1", "UTF-8//TRANSLIT", "ol\303\251")
test_equal("ol\351", result)
result = Iconv.conv("ISO-8859-1", "UTF-8//IGNORE", "ol\303\251")
test_equal("ol\351", result)
result = Iconv.conv("ISO-8859-1", "UTF-8//IGNORE//TRANSLIT", "ol\303\251")
test_equal("ol\351", result)

