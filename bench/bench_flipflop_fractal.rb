require 'benchmark'

def fractal_flipflop
  w, h = 44, 54
  c = 7 + 42 * w
  a = [0] * w * h
  g = d = 0
  f = proc do |n|
    a[c] += 1
    o = a.map {|z| " :#"[z, 1] * 2 }.join.scan(/.{#{w * 2}}/)
    puts "\f" + o.map {|l| l.rstrip }.join("\n")
    d += 1 - 2 * ((g ^= 1 << n) >> n)
    c += [1, w, -1, -w][d %= 4]
  end
  1024.times do
    !!(!!(!!(!!(!!(!!(!!(!!(!!(true...
      f[0])...f[1])...f[2])...
      f[3])...f[4])...f[5])...
      f[6])...f[7])...f[8])
  end
end

(ARGV[0] || 5).to_i.times do
  puts Benchmark.measure { fractal_flipflop }
end