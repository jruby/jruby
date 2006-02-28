require 'test/minirunit'
require 'stringio'

f = open("testStringIO_tmp", "w")
f.write("one\ntwo\nthree\n")
f.close

strio = StringIO.new(IO.read("testStringIO_tmp"))

lines = []
3.times {lines << strio.readline}

test_equal(["one", "two", "three"], lines)

File.delete("testStringIO_tmp")