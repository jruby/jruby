require 'benchmark'

class BenchByteList
# These match the strings used in TestByteList.java,
# though the tests here are different.
EMPTY = "";
VERY_SHORT = "abc"                        # 3 chars
SHORT = "abcdefgh"                        # 8 chars
MEDIUM = "0abcdefghi1ABCDEFGHI2123456789" # 30 chars
LONG = MEDIUM * 4                         # 120 chars
VERY_LONG = LONG * 4                      # 480 chars
SHORT_ARG = "zyxw"                        # 4 chars
MEDIUM_ARG = SHORT_ARG + "vutsrqpo"       # 12 chars
LONG_ARG = MEDIUM_ARG + "nmlkjihgfedcba"  # 26 chars

class << self
def bench_string_sort(loops,str='test this string')
  arr = []
  5000.times do
    arr << str + (rand(10000)).to_s
  end
  puts "Measure string array sort time"
  5.times { 
    puts Benchmark.measure { 
      loops.times { 
        arr.sort 
      } 
    } 
  }
end

def bench_hash_put(loops,str='test this string')
  arr = []
  500.times do
    arr << str + (rand(10000)).to_s
  end
  puts "Measure hash put time"
  5.times { 
    puts Benchmark.measure { 
      loops.times { 
        hash = {}
        arr.each do |key|
          hash[key] = :value
        end
      } 
    } 
  }
end

def bench_hash_get(loops,str='test this string')
  hash = {}
  arr = []
  500.times do
    key = str + (rand(10000)).to_s
    arr << key
    hash[key] = :value
  end
  puts "Measure hash get time (note: not same scale as put test)"
  5.times { 
    puts Benchmark.measure { 
      loops.times { 
        t = nil
        arr.each do |key|
          hash[key]
          hash[key]
          hash[key]
          hash[key]
          hash[key]
          hash[key]
          hash[key]
          hash[key]
          hash[key]
          hash[key]
        end
      } 
    } 
  }
end

def bench_string_eq(loops)
  empty1 = EMPTY
  empty2 = EMPTY.dup
  vshort1 = VERY_SHORT
  vshort2 = VERY_SHORT.dup
  short1 = SHORT
  short2 = SHORT.dup
  medium1 = MEDIUM
  medium2 = MEDIUM.dup
  long1 = LONG
  long2 = LONG.dup
  vlong1 = VERY_LONG
  vlong2 = VERY_LONG.dup
  
  puts "Measure string == comparison time"
  5.times { 
    puts Benchmark.measure { 
      eq = nil
      (loops*500).times {
          empty1 == empty2
          vshort1 == vshort2
          short1 == short2
          medium1 == medium2
          long1 == long2
          vlong1 == vlong2
          empty1 == empty2
          vshort1 == vshort2
          short1 == short2
          medium1 == medium2
          long1 == long2
          vlong1 == vlong2
      }
    } 
  }
end

def bench_string_not_eq(loops)
  single1 = 'A'
  single2 = 'B'
  vshort1 = VERY_SHORT + 'A'
  vshort2 = VERY_SHORT + 'B'
  short1 = SHORT + 'A'
  short2 = SHORT + 'B'
  medium1 = MEDIUM + 'A'
  medium2 = MEDIUM + 'B'
  long1 = LONG + 'A'
  long2 = LONG + 'B'
  vlong1 = VERY_LONG + 'A'
  vlong2 = VERY_LONG + 'B'
  
  puts "Measure string == comparison time, different last pos"
  5.times { 
    puts Benchmark.measure { 
      eq = nil
      (loops*500).times {
          single1 == single2
          vshort1 == vshort2
          short1 == short2
          medium1 == medium2
          long1 == long2
          vlong1 == vlong2
          single1 == single2
          vshort1 == vshort2
          short1 == short2
          medium1 == medium2
          long1 == long2
          vlong1 == vlong2
      }
    } 
  }
end

def bench_string_cmp(loops)
  empty1 = EMPTY
  empty2 = EMPTY.dup
  vshort1 = VERY_SHORT
  vshort2 = VERY_SHORT.dup
  short1 = SHORT
  short2 = SHORT.dup
  medium1 = MEDIUM
  medium2 = MEDIUM.dup
  long1 = LONG
  long2 = LONG.dup
  vlong1 = VERY_LONG
  vlong2 = VERY_LONG.dup
  puts "Measure string <=> comparison time"
  5.times { 
    puts Benchmark.measure { 
      (loops*500).times {
          empty1 <=> empty2
          vshort1 <=> vshort2
          short1 <=> short2
          medium1 <=> medium2
          long1 <=> long2
          vlong1 <=> vlong2
          empty1 <=> empty2
          vshort1 <=> vshort2
          short1 <=> short2
          medium1 <=> medium2
          long1 <=> long2
          vlong1 <=> vlong2
      }
    } 
  }
end

def bench_cmp_expensive_unassigned_strings!(loops)
  puts "Unassigned strings are expensive! (e.g, 'xxxx' <=> 'yyyy')"
  5.times { 
    puts Benchmark.measure { 
      cmp = nil
      (loops*500).times {
        'xxxxxxxxxa' <=> 'xxxxxxxxxb'
        'xxxxxxxxxa' <=> 'xxxxxxxxxb'
        'xxxxxxxxxa' <=> 'xxxxxxxxxb'
        'xxxxxxxxxa' <=> 'xxxxxxxxxb'
        'xxxxxxxxxa' <=> 'xxxxxxxxxb'
        'xxxxxxxxxa' <=> 'xxxxxxxxxb'
        'xxxxxxxxxa' <=> 'xxxxxxxxxb'
        'xxxxxxxxxa' <=> 'xxxxxxxxxb'
        'xxxxxxxxxa' <=> 'xxxxxxxxxb'
        'xxxxxxxxxa' <=> 'xxxxxxxxxb'
      }
    } 
  }
end

def bench_string_index_char(loops)
  arr = []
  0.upto(250) do |i|
    arr << ('x' * i + '.' + 'x')
  end
  puts "Measure 'string'.index(fixnum) time"
  5.times { 
    puts Benchmark.measure { 
      idx = nil
      loops.times { 
        0.upto(250) do |i|
          val = arr[i]
          val.index(46) # 46 => '.'
          val.index(46)
          val.index(46)
          val.index(46)
          val.index(46)
          val.index(46)
          val.index(46)
          val.index(46)
          val.index(46)
          val.index(46)
        end
      } 
    } 
  }
end

def bench_string_index_str(loops)
  chars = 'abcdefghijklmnopqrstuvwxyz'
  search = []
  1.upto(26) do |i|
    search << chars[0,i]
  end
  arr = []
  0.upto(250) do |i|
    arr << ('x' * i + search[i%26] + 'x')
  end
  puts "Measure 'string'.index(str) time"
  5.times { 
    puts Benchmark.measure { 
      loops.times { 
        0.upto(250) do |i|
          val = arr[i]
          srch = search[i%26]
          val.index(srch)
          val.index(srch)
          val.index(srch)
          val.index(srch)
          val.index(srch)
          val.index(srch)
          val.index(srch)
          val.index(srch)
          val.index(srch)
          val.index(srch)
        end
      } 
    } 
  }
end

def bench_string_rindex_char(loops)
  arr = []
  0.upto(250) do |i|
    arr << ('x' + '.' + ('x' * i))
  end
  puts "Measure 'string'.rindex(fixnum) time"
  5.times { 
    puts Benchmark.measure { 
      loops.times { 
        0.upto(250) do |i|
          val = arr[i]
          val.rindex(46) # 46 => '.'
          val.rindex(46)
          val.rindex(46)
          val.rindex(46)
          val.rindex(46)
          val.rindex(46)
          val.rindex(46)
          val.rindex(46)
          val.rindex(46)
          val.rindex(46)
        end
      } 
    } 
  }
end

def bench_string_rindex_str(loops)
  chars = 'abcdefghijklmnopqrstuvwxyz'
  search = []
  1.upto(26) do |i|
    search << chars[0,i]
  end
  arr = []
  0.upto(250) do |i|
    arr << ('x' + search[i%26] + ('x' * i))
  end
  puts "Measure 'string'.rindex(str) time"
  5.times { 
    puts Benchmark.measure { 
      loops.times { 
        0.upto(250) do |i|
          val = arr[i]
          srch = search[i%26]
          val.rindex(srch)
          val.rindex(srch)
          val.rindex(srch)
          val.rindex(srch)
          val.rindex(srch)
          val.rindex(srch)
          val.rindex(srch)
          val.rindex(srch)
          val.rindex(srch)
          val.rindex(srch)
        end
      } 
    } 
  }
end

def run(loops=1000)
  bench_string_sort(loops)
  bench_hash_put(loops)
  bench_hash_get(loops)
  bench_string_eq(loops)
  bench_string_not_eq(loops)
  bench_string_cmp(loops)
  #bench_cmp_expensive_unassigned_strings!(loops)
  bench_string_index_char(loops)
  bench_string_index_str(loops)
  bench_string_rindex_char(loops)
  bench_string_rindex_str(loops)
end

end #self
end #ByteListBench
BenchByteList.run
