require 'test/minirunit'
test_check "Test Digest module:"

require "digest/md5"
require "digest/sha1"

a = Digest::MD5.new
a << "f"
a.update "o"
a << "o"

test_equal("acbd18db4cc2f85cedef654fccc4a4d8", a.hexdigest)
test_equal("acbd18db4cc2f85cedef654fccc4a4d8",  Digest::MD5.hexdigest("foo"))
test_equal("\254\275\030\333L\302\370\\\355\357eO\314\304\244\330", Digest::MD5.digest("foo"))
test_equal("0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33", Digest::SHA1.hexdigest("foo"))
