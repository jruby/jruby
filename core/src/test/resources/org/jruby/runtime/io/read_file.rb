open(ARGV[0]) do |f|
  loop do
    f.sysread(1024)
  end
end