require 'benchmark'

DATA_FILE1 = File.join(File.expand_path(File.dirname(__FILE__)), "load_data.rb")
DATA_FILE2 = File.join(File.expand_path(File.dirname(__FILE__)), "..", "lib/ruby/1.8", "rational.rb")

(ARGV[0] || 1).to_i.times do
  Benchmark.bm(35) do |bm|
    # useful to see how we're doing during startup,
    # when lots of cold require calls are made.
    bm.report("  1 load 'fileutils-like'") {
        load DATA_FILE1
    }

    bm.report(" 1K load 'fileutils-like'") {
      1_000.times {
        load DATA_FILE1
      }
    }

    bm.report(" 1K load 'rational'") {
      1_000.times {
        begin
          load DATA_FILE2
        rescue LoadError
          p 5
        end
      }
    }

    bm.report("10K require 'non-existing'") {
      (1..10_000).each { |i|
        begin
          require 'stuff' + i.to_s
        rescue LoadError
        end
      }
    }
  end
end
