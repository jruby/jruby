def fib_ruby(n)
 (n < 2) ? n : fib_ruby(n - 2) + fib_ruby(n - 1)
end
