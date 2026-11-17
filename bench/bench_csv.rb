# Usage: jruby csv_bench.rb {ANYTHING}?.  ANYTHING old csv. nothing is new one.

if ARGV.shift
  puts 'Using 1.7 csv'
  require_relative 'old_csv'
else
  puts 'Using 9.4 csv'
  require 'csv'
end

require 'benchmark'

dir = "data"
Dir.mkdir(dir) unless Dir.exist?(dir)

input = "#{dir}/extract.csv"

unless File.exist?(input)
  Dir.chdir(dir) do
    zip = "sirene_2017111_E_Q.zip"
    system "wget", "-O", zip, "http://files.data.gouv.fr/sirene/#{zip}"
    system "unzip", zip
    system "head -10000 sirc* > extract.csv"
  end
end

loop do
  p Benchmark.realtime {
    CSV.foreach(input, col_sep: ';', encoding: "ISO-8859-1:UTF-8") { |row|
    }
  }
end
