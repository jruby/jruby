require 'benchmark'

Benchmark.bm(30) do |bm|
  5.times do
    bm.report('5000 reads of __FILE__') do
      5000.times { File.open(__FILE__) {|f| while f.gets; end} }
    end
  end
end

=begin A bunch of crap
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
foo bar baz
=end
