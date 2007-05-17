require 'test/minirunit'
test_check "Iconv:"

require 'iconv'

result = Iconv.conv("UTF-16", "ISO-8859-1", "hello")
test_equal(12, result.length)
test_equal("\376\377\000h\000e\000l\000l\000o", result)

result = Iconv.conv("ISO-8859-1", "UTF-8", "ol\303\251")
test_equal("ol\351", result)

# FIXME: Not implemented yet (though core work done to support it) [JRUBY-309]
#test_exception(Iconv::IllegalSequence) { p Iconv.conv("ASCII", "UTF-8", "ol\303\251") }

# FIXME: Java does not support transliteration in core jdk?
#Iconv.iconv("ASCII//TRANSLIT", "UTF-8", "ol\303\251")

