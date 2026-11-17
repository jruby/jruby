require 'benchmark/ips'

Benchmark.ips do |x|

  x.config(:time => 10, :warmup => 10)

  x.report("raise(msg) default") do
    begin
      raise 'default'
    rescue => ex
      ex && true
    end
  end

  x.report("raise(class, msg)") do
    begin
      raise RuntimeError, 'def'
    rescue => ex
      ex && true
    end
  end

  x.report("raise(class, msg, nil)") do
    begin
      raise RuntimeError, 'nil', nil
    rescue => ex
      ex && true
    end
  end

  trace = caller.dup
  x.report("raise(class, msg, trace)") do
    begin
      raise RuntimeError, 'nil', backtrace
    rescue => ex
      ex && true
    end
  end

  x.report("raise(class, msg) rescue nil") do
    (raise RuntimeError, 'def') rescue nil
  end

  x.report("raise(class, msg, trace) rescue nil") do
    (raise RuntimeError, 'def', trace) rescue nil
  end

end