require 'benchmark/ips'

FLOATS = {
  "0.1 + 0.2" => 0.1 + 0.2,
  "random" => 0.8444218515250481,
  "1.5" => 1.5,
  "1e100" => 1e100,
  "123456.789" => 123456.789,
}

EMBEDDING = Array.new(1536) { rand }

Benchmark.ips do |bm|
  FLOATS.each do |name, f|
    bm.report("Float#to_s #{name}") do |n|
      i = 0
      while i < n
        f.to_s
        i += 1
      end
    end
  end

  bm.report("Array#inspect of 1,536 Floats") do |n|
    i = 0
    while i < n
      EMBEDDING.inspect
      i += 1
    end
  end

  bm.report("Marshal.dump of 1,536 Floats") do |n|
    i = 0
    while i < n
      Marshal.dump(EMBEDDING)
      i += 1
    end
  end
end
