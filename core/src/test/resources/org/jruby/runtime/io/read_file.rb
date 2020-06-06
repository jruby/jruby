open(ARGV[0]) do |f|
  loop do
    x = f.sysread(1024)
    puts "read #{x.length}"
  end
end