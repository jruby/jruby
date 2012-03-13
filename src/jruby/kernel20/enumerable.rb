module Enumerable
  def lazy
    Lazy.new(self)
  end

  class Lazy < Enumerator
    def initialize(obj, &block)
      super(){|yielder|
        begin
          obj.each{|x|
            if block
              block.call(yielder, x)
            else
              yielder << x
            end
          }
        rescue StopIteration
        end
      }
    end

    def map(&block)
      Lazy.new(self){|yielder, val|
        yielder << block.call(val)
      }
    end
    alias collect map

    def select(&block)
      Lazy.new(self){|yielder, val|
        if block.call(val)
          yielder << val
        end
      }
    end
    alias find_all select

    def reject(&block)
      Lazy.new(self){|yielder, val|
        if not block.call(val)
          yielder << val
        end
      }
    end

    def grep(pattern)
      Lazy.new(self){|yielder, val|
        if pattern === val
          yielder << val
        end
      }
    end
  end
end