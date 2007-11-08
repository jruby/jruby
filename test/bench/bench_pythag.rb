require 'benchmark'

def pythag(n)
  result = []
  (2..n).each do |c|
    (1...c).each do |b|
      a = Math.sqrt(c*c - b*b)
      result << [a.to_i, b, c] if a.to_i == a
    end
  end
  result
end

5.times { puts Benchmark.measure { pythag(5000) } }
