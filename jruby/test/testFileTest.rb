require 'test/minirunit'

test_check "Test FileTest"

test_ok(FileTest.file?('test/testFile.rb'))
test_ok(! FileTest.file?('test'))

