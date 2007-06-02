require 'test/minirunit'
test_check "Test string evaluation:"

#########    test1   #################
test_ok('Seconds/day: 86400' == "Seconds/day: #{24*60*60}")

#########    test2   #################
test_ok('Ho! Ho! Ho! Merry Christmas' == "#{'Ho! '*3}Merry Christmas" )

#########    test3   #################
test_ok('Say: Ho! Ho! Ho! Merry Christmas' == "Say: " "#{'Ho! '*3}Merry Christmas" )
test_ok('Ho! Ho! Ho! Ho! Ho! Ho! ' == "#{'Ho! '*3}" "#{'Ho! '*3}" )

