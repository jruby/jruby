require 'benchmark'

aTime = Time.now

(ARGV[0] || 2).to_i.times do
  Benchmark.bm(35) do |bm|

    ENV['TZ']=nil
    bm.report("5m Time.at") { 5_000_000.times { Time.at(1000) }}

    ENV['TZ']="EDT+5"
    bm.report("5m Time.at, TZ set") { 5_000_000.times { Time.at(1000) }}

    ENV['TZ']=nil
    bm.report("5m Time.at(Time)") { 5_000_000.times { Time.at(aTime) }}

    ENV['TZ']=nil
    bm.report("5m Time.now") { 5_000_000.times { Time.now }}

    ENV['TZ']="EDT+5"
    bm.report("5m Time.now, TZ set") { 5_000_000.times { Time.now }}

    ENV['TZ']=nil
    bm.report("5m Time.local, string args") {
      5_000_000.times {
        Time.local('2000', 'jan', '1', '20', '15' ,'1', '0')
      }
    }

    bm.report("5m Time.local, numeric args") {
      5_000_000.times {
        Time.local(2000, 5, 1, 20, 15 , 1, 0)
      }
    }

    bm.report("5m Time.gm, string args") {
      5_000_000.times {
        Time.gm('2000', 'jan', '1', '20', '15' ,'1', '0')
      }
    }

    bm.report("5m Time.gm, numeric args") {
      5_000_000.times {
        Time.gm(2000, 5, 1, 20, 15 , 1, 0)
      }
    }

    bm.report("10m ENV['stuff']") { 10_000_000.times { ENV['stuff']}}

  end
end
