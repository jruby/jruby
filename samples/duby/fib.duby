import "System", "java.lang.System"

def fib(a => :fixnum)
  if a < 2
    a
  else
    fib(a - 1) + fib(a - 2)
  end
end

time_start = System.currentTimeMillis
puts "fib(45):"
puts fib(45)
time_total = System.currentTimeMillis - time_start
puts "Total time:"
puts time_total
