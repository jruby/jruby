require 'benchmark'


SIMPLE_TIMES = 10_000_000
GROWTH_TIMES =     8_000

# prepare dummy objects
objects = (0..20).map{Class.new.new}

simple_reader = proc{|o| result = nil;SIMPLE_TIMES.times{ result = o.instance_variable_get(:@foo)};result}
simple_writer = proc{|o| SIMPLE_TIMES.times{o.instance_variable_set(:@foo,1)}}


Benchmark.bmbm do |b|
  b.report "baseline x#{SIMPLE_TIMES}" do
    SIMPLE_TIMES.times{1}
  end

  b.report "single threaded reads x#{SIMPLE_TIMES}" do
    o = objects.pop;
    o.instance_variable_set(:@foo,1)
    simple_reader.call(o)
  end
  
  b.report "single threaded writes x#{SIMPLE_TIMES}" do
    o = objects.pop
    simple_writer.call(o)
  end
  
  b.report "two reader threads x#{SIMPLE_TIMES}" do
    o = objects.pop;
    o.instance_variable_set(:@foo,1)
    
    t1 = Thread.new{simple_reader.call(o)}
    t2 = Thread.new{simple_reader.call(o)}
    t1.join;t2.join
  end
  
  b.report "one reader, one writer x#{SIMPLE_TIMES}" do
    o = objects.pop
    t1 = Thread.new{simple_reader.call(o)}
    t2 = Thread.new{simple_writer.call(o)}
    t1.join;t2.join
  end

  b.report "single threaded growth x#{GROWTH_TIMES}" do
    o = objects.pop
    (0..GROWTH_TIMES).each{|i| o.instance_variable_set(:"@bar_#{i}",1)}
  end
  

end
