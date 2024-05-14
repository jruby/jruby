require 'benchmark/ips'

class Stringable
  def to_s
    "quux".freeze
  end
end

Benchmark.ips {|bm|
  bm.report('"#{n}" fixnum') {|n|
    a = nil
    while n > 0
      n-=1
      a = "#{n}"
    end
    a
  }

  bm.report('"foo#{n}" fixnum') {|n|
    a = nil
    while n > 0
      n-=1
      a = "foo#{n}"
    end
    a
  }

  bm.report('"#{n}bar" fixnum') {|n|
    a = nil
    while n > 0
      n-=1
      a = "#{n}bar"
    end
    a
  }
  bm.report('"#{n}#{n}" fixnum') {|n|
    a = nil
    while n > 0
      n-=1
      a = "#{n}#{n}"
    end
    a
  }

  bm.report('"foo#{n}bar" fixnum') {|n|
    a = nil
    while n > 0
      n-=1
      a = "foo#{n}bar"
    end
    a
  }

  bm.report('"#{n}bar#{n}" fixnum') {|n|
    a = nil
    while n > 0
      n-=1
      a = "#{n}bar#{n}"
    end
    a
  }

  bm.report('"#{n}#{n}bar" fixnum') {|n|
    a = nil
    while n > 0
      n-=1
      a = "#{n}#{n}bar"
    end
    a
  }

  bm.report('"foo#{n}#{n}" fixnum') {|n|
    a = nil
    while n > 0
      n-=1
      a = "foo#{n}#{n}"
    end
    a
  }

  bm.report('"#{n}#{n}#{n}" fixnum') {|n|
    a = nil
    while n > 0
      n-=1
      a = "#{n}#{n}#{n}"
    end
    a
  }

  bm.report('"foo#{n}bar#{n}baz" fixnum') {|n|
    a = nil
    while n > 0
      n-=1
      a = "foo#{n}bar#{n}baz"
    end
    a
  }

  bm.report('"foo#{n}bar#{n}baz#{n}" fixnum') {|n|
    a = nil
    while n > 0
      n-=1
      a = "foo#{n}bar#{n}baz"
    end
    a
  }

  bm.report('"#{x}#{x}#{x}#{x}#{x}" to_s') {|n|
    a = nil
    x = Stringable.new
    while n > 0
      n-=1
      a = "#{x}#{x}#{x}#{x}#{x}"
    end
    a
  }

  bm.report('"#{x}#{x}#{x}#{x}#{x}#{x}#{x}#{x}#{x}#{x}" to_s') {|n|
    a = nil
    x = Stringable.new
    while n > 0
      n-=1
      a = "#{x}#{x}#{x}#{x}#{x}#{x}#{x}#{x}#{x}#{x}"
    end
    a
  }
}
