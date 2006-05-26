require 'test/minirunit'

test_check "Test IO"

@file = "TestIO_tmp"
@file2 = "Test2IO_tmp"
@file3 = "Test3IO_tmp"

test_exception(ArgumentError) { IO.new }
test_exception(TypeError) { IO.new "FROGGER" }
test_exception(TypeError) { IO.foreach 3 }

# Test for appropriate handling of default gets delimiting
f = File.new(@file3, "w")
f.print("A\n\n\nB\n")
f.close
f = File.new(@file3, "r")
a = f.gets("\n\n")
b = f.gets("\n\n")
c = f.gets("\n\n")
f.close
test_ok(b == "\nB\n", "gets of non-paragraph \"\\n\\n\" failed")

# Two ios with same fileno, but different objects.
f = File.new(@file, "w")
f.puts("heh")
g = IO.new(f.fileno)
test_equal(f.fileno, g.fileno)
test_exception(IOError) { g.gets }
g.close
test_exception(IOError) { g.puts }

f = File.new(@file, "r")
g = IO.new(f.fileno)
test_equal(f.fileno, g.fileno)
test_exception(IOError) { g.puts }
# If g closes then g knows that it was once a valid descriptor.
# So it throws an IOError.
g.close
test_exception(IOError) { g.gets }

# In this case we will have f close (which will pull the rug
# out from under g) and thus make g try the ops and fail
f = File.open(@file)
g = IO.new(f.fileno)
f.close
test_exception(Errno::EBADF) { g.readchar }
test_exception(Errno::EBADF) { g.readline }
test_exception(Errno::EBADF) { g.gets }
test_exception(Errno::EBADF) { g.close }
test_exception(IOError) { g.getc }
test_exception(IOError) { g.readchar }
test_exception(IOError) { g.read }
test_exception(IOError) { g.sysread 1 }

f = File.open(@file, "w")
g = IO.new(f.fileno)
f.close
test_no_exception { g.print "" }
test_no_exception { g.write "" }
test_no_exception { g.puts "" }
test_no_exception { g.putc 'c' }
test_exception(Errno::EBADF) { g.syswrite "" }

# Cannot open an IO which does not have compatible permission with
# original IO
f = File.new(@file2, "w")
test_exception(Errno::EINVAL) { g = IO.new(f.fileno, "r") }
f.close

f = File.new(@file, "r")
test_exception(Errno::EINVAL) { g = IO.new(f.fileno, "w") }
f.close

# However, you can open a second with less permissions
f = File.new(@file, "r+")
g = IO.new(f.fileno, "r")
g.gets
f.puts "HEH"
test_exception(IOError) { g.write "HOH" }
test_equal(f.fileno, g.fileno)
f.close

f = File.new(@file)
test_exception(Errno::EINVAL) { f.seek(-1) }

# Advance one + single arg seek
f.seek(1)
test_equal(f.pos, 1)
f.close

test_exception(Errno::ESPIPE) { $stdin.seek(10) }

# empty write...writes nothing and does not complain
f = File.new(@file, "w")
i = f.syswrite("")
test_equal(i, 0)
i = f.syswrite("heh")
test_equal(i, 3)
f.close

test_exception(Errno::ENOENT) { File.foreach("nonexistent_file") {} }

f = File.open(@file)
f.gets
i = File.open(@file2)
t = i.fileno;
i = i.reopen(f)
test_equal(f.pos, i.pos)
test_equal(t, i.fileno);
test_ok(f.fileno != i.fileno);
i.close
f.close

f = File.open(@file, "w")
f.puts("line1");
f.puts("line2");
f.puts("line3");
f.close

f = File.open(@file)
test_equal(f.gets(), $_)
test_equal(f.readline(), $_)
f.close

# test that read returns correct values
f = File.open(@file)
f.read # read all
test_equal("", f.read)
test_equal(nil, f.read(1))
f.close

# Test deleting files
test_ok(File.delete(@file, @file2, @file3))

##### select #####
test_equal(nil, select(nil, nil, nil, 0))
test_exception(ArgumentError) { select(nil, nil, nil, -1) }

