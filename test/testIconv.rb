require 'test/minirunit'
test_check "Iconv:"

require 'iconv'

test_exception(Iconv::IllegalSequence) { p Iconv.conv("ASCII", "UTF-8", "ol\303\251") }
test_exception(Iconv::IllegalSequence) { p Iconv.conv("UTF-8", "UTF-8", "\xa4") }
test_exception(Iconv::IllegalSequence) { p Iconv.conv("UTF-8", "UTF-8//IGNORE", "\xa4") }

# FIXME: Java does not support transliteration in core jdk?
#Iconv.iconv("ASCII//TRANSLIT", "UTF-8", "ol\303\251")

# JRUBY-867, make us ignore transliteration stuff since we can't support it

result = Iconv.conv("ISO-8859-1//TRANSLIT", "UTF-8", "ol\303\251")
test_equal("ol\351", result)
result = Iconv.conv("ISO-8859-1//IGNORE", "UTF-8", "ol\303\251")
test_equal("ol\351", result)
result = Iconv.conv("ISO-8859-1//IGNORE//TRANSLIT", "UTF-8", "ol\303\251")
test_equal("ol\351", result)

result = Iconv.conv("UTF-8//IGNORE", "UTF-8", "\xa4")
test_equal("", result)