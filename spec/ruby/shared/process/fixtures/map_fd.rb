fd = ARGV.shift.to_i

f = File.for_fd fd
f.sync = true

f.write "writing to fd: #{fd}"
