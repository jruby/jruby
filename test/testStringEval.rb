require 'minirunit'
test_check "Test string evaluation:"

#########    test1   #################
test_ok('Seconds/day: 86400' == "Seconds/day: #{24*60*60}")

#########    test2   #################
test_ok('Ho! Ho! Ho! Merry Christmas' == "#{'Ho! '*3}Merry Christmas" )

