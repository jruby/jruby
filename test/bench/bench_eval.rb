require 'benchmark'

amount = 1000000
c = 0
script = <<EOF
_e = ''; _e.concat '  The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is:   The value of x is: '; _e.concat(( x ).to_s); _e.concat "\n"
_e
EOF

x = 1
bnd = binding

puts "implicit binding"
puts Benchmark.realtime { amount.times { eval script } }
puts "c = #{c}"
puts "explicit binding"
puts Benchmark.realtime { amount.times { eval script, bnd } }
puts "c = #{c}"
