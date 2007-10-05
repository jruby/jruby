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

E = A::B::C::D::E
j = J.new
k = E::K.new

Benchmark.bm(40) do |bm|
  5.times { bm.report("100k * 100 nested const get") { E::bench }}
  5.times { bm.report("100k * 100 inherited const get") { j.bench }}
  5.times { bm.report("100k * 100 both") { k.bench }}
end