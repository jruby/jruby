def m(foo:, bar:)
  foo + bar
end

def n(foo = 1, bar = 2)
  foo + bar
end

10.times.each do |times|
#    beginning_time = Time.now
#    (1..100_000_000).each do |i|
#        m()
#    end
#    end_time = Time.now
#    puts "Time elapsed #{(end_time - beginning_time)*1000} milliseconds"

    beginning_time = Time.now
    (1..1000000).each do |i|
        #n(1, 2)
        m(foo: 1, bar: 2)
    end
    end_time = Time.now
    puts "Time elapsed #{(end_time - beginning_time)*1000} milliseconds"
end
