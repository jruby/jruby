require 'benchmark'


class F
  FOO2 = 1
end
class G < F; end
class H < G; end
class I < H; end
class J < I
  def bench
    100000.times do
      FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
      FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
      FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
      FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
      FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
      FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
      FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
      FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
      FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
      FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
    end
  end
end

module A
  FOO = 1
  module B
    module C
      module D
        module E
          def bench
            100000.times do
              FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO
              FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO
              FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO
              FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO
              FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO
              FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO
              FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO
              FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO
              FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO
              FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO; FOO
            end
          end
          module_function :bench
          
          class K < I
            def bench
              100000.times do
                FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
                FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
                FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
                FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
                FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
                FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
                FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
                FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
                FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
                FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2; FOO2
              end
            end
          end
        end
      end
    end
  end
end

def bench_const_lookup(bm)
  oldbm = $bm
  $bm = bm
  class << self
    F = A::B::C::D::E
    j = J.new
    k = F::K.new

    5.times { $bm.report("100k * 100 nested const get") { F::bench }}
    5.times { $bm.report("100k * 100 inherited const get") { j.bench }}
    5.times { $bm.report("100k * 100 both") { k.bench }}
  end
  $bm = oldbm
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_const_lookup(bm)} }
end
