require 'benchmark/ips'

# Modified version of benchmark from https://github.com/jruby/jruby/issues/3419

class Test
  def intitialize
    @ivar = 'test'
  end

  def direct
    @ivar
  end

  IVAR_NAME = "@ivar"
  def hoist_ivar_name
    instance_variable_get IVAR_NAME
  end

  def instance_variable_get_symbol
    instance_variable_get :"@ivar"
  end

  def instance_variable_get_frozen
    instance_variable_get('@ivar'.freeze)
  end

  def instance_variable_get_frozen_symbol
    instance_variable_get('@ivar'.freeze.to_sym)
  end
end

loop {
Benchmark.ips do |benchmark|
  instance = Test.new
  benchmark.report("direct") {|i|
    while i > 0
      i-=1
      instance.direct
    end
  }
  benchmark.report("hoist_ivar_name") {|i|
    while i > 0
      i-=1
      instance.hoist_ivar_name
    end
  }
  benchmark.report("ivar get symbol") {|i|
    while i > 0
      i-=1
      instance.instance_variable_get_symbol
    end
  }
  benchmark.report("ivar get frozen") {|i|
    while i > 0
      i-=1
      instance.instance_variable_get_frozen
    end
  }
  benchmark.report("ivar get frozen sym") {|i|
    while i > 0
      i-=1
      instance.instance_variable_get_frozen_symbol
    end
  }
end
}