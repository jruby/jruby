require 'benchmark/ips'

def array0
  []
end

def fixnum_array1
  [1]
end

def fixnum_array2
  [1,2]
end

def fixnum_array3
  [1,2,3]
end

def fixnum_array10
  [1,2,3,4,5,6,7,8,9,10]
end

def float_array1
  [1.0]
end

def float_array2
  [1.0,2.0]
end

def float_array3
  [1.0,2.0,3.0]
end

def float_array10
  [1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,9.0,10.0]
end

Benchmark.ips do |bm|
  bm.report("empty array") do |i|
    n = 0
    while i > 0
      i-=1
      n += array0.size
    end
    n
  end

  bm.report("fixnum array 1") do |i|
    n = 0
    while i > 0
      i-=1
      n += fixnum_array1.size
    end
    n
  end

  bm.report("fixnum array 2") do |i|
    n = 0
    while i > 0
      i-=1
      n += fixnum_array2.size
    end
    n
  end

  bm.report("fixnum array 3") do |i|
    n = 0
    while i > 0
      i-=1
      n += fixnum_array3.size
    end
    n
  end

  bm.report("fixnum array 10") do |i|
    n = 0
    while i > 0
      i-=1
      n += fixnum_array10.size
    end
    n
  end

  bm.report("float array 1") do |i|
    n = 0
    while i > 0
      i-=1
      n += float_array1.size
    end
    n
  end

  bm.report("float array 2") do |i|
    n = 0
    while i > 0
      i-=1
      n += float_array2.size
    end
    n
  end

  bm.report("float array 3") do |i|
    n = 0
    while i > 0
      i-=1
      n += float_array3.size
    end
    n
  end

  bm.report("float array 10") do |i|
    n = 0
    while i > 0
      i-=1
      n += float_array10.size
    end
    n
  end
end