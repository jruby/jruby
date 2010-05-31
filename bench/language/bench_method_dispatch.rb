require 'benchmark'

def bench_method_dispatch(bm)
  bm.report "control: 10m local var access" do
    a = 5; 
    i = 0;
    while i < 100000
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      i += 1;
    end
  end

  bm.report "core: 10m Fixnum#to_i" do
    a = 5; 
    i = 0;
    while i < 100000
      a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
      a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
      a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
      a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
      a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
      a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
      a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
      a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
      a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
      a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
      i += 1;
    end
  end

  oldbm = $bm
  $bm = bm
  def foo
    self
  end

  def foo1(arg)
    self
  end

  def foo2(arg, arg2)
    self
  end

  def foo3(arg, arg2, arg3)
    self
  end

  def foo4(arg, arg2, arg3, arg4)
    self
  end

  def foos(*args)
    self
  end
  
  class << self
    define_method(:bar) { }
  end

  class << self
    define_method(:bar1) {|a| }
  end

  class << self
    define_method(:bar2) {|a,b| }
  end

  class << self
    define_method(:bars) {|*a| }
  end

  def optfix(opts = 1)
    self
  end

  def optary(opts = [])
    self
  end
  
  def opthash(opts = { })
    self
  end

  def quux(&block)
    self
  end
  
  def invoke_foo
    i = 0;
    while i < 100000
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
      foo; foo; foo; foo; foo; foo; foo; foo; foo; foo;
      i += 1;
    end
  end

  $bm.report "ruby: 10m def foo() with foo()" do
    invoke_foo
  end
  
  def invoke_foos
    i = 0;
    while i < 100000
      foos; foos; foos; foos; foos; foos; foos; foos; foos; foos; 
      foos; foos; foos; foos; foos; foos; foos; foos; foos; foos; 
      foos; foos; foos; foos; foos; foos; foos; foos; foos; foos; 
      foos; foos; foos; foos; foos; foos; foos; foos; foos; foos; 
      foos; foos; foos; foos; foos; foos; foos; foos; foos; foos; 
      foos; foos; foos; foos; foos; foos; foos; foos; foos; foos; 
      foos; foos; foos; foos; foos; foos; foos; foos; foos; foos; 
      foos; foos; foos; foos; foos; foos; foos; foos; foos; foos; 
      foos; foos; foos; foos; foos; foos; foos; foos; foos; foos; 
      foos; foos; foos; foos; foos; foos; foos; foos; foos; foos; 
      i += 1;
    end
  end

  $bm.report "ruby: 10m def foo(*a) with foo()" do
    invoke_foos
  end
  
  def invoke_foos_nil
    i = 0;
    while i < 100000
      foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); 
      foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); 
      foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); 
      foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); 
      foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); 
      foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); 
      foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); 
      foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); 
      foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); 
      foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); foos(nil); 
      i += 1;
    end
  end

  $bm.report "ruby: 10m def foo(*a) with foo(nil)" do
    invoke_foos_nil
  end
  
  def invoke_foos_nil4
    i = 0;
    while i < 100000
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); foos(nil, nil, nil, nil); 
      i += 1;
    end
  end

  $bm.report "ruby: 10m def foo(*a) with foo(nil*4)" do
    invoke_foos_nil4
  end
  
  def invoke_foo1
    i = 0;
    while i < 100000
      foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); 
      foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); 
      foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); 
      foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); 
      foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); 
      foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); 
      foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); 
      foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); 
      foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); 
      foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); foo1(nil); 
      i += 1;
    end
  end

  $bm.report "ruby: 10m def foo(a)" do
    invoke_foo1
  end
  
  def invoke_foo2
    i = 0;
    while i < 100000
      foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); 
      foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); 
      foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); 
      foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); 
      foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); 
      foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); 
      foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); 
      foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); 
      foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); 
      foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); foo2(nil, nil); 
      i += 1;
    end
  end

  $bm.report "ruby: 10m def foo(a,b)" do
    invoke_foo2
  end
  
  def invoke_foo3
    i = 0;
    while i < 100000
      foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); 
      foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); 
      foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); 
      foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); 
      foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); 
      foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); 
      foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); 
      foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); 
      foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); 
      foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); foo3(nil, nil, nil); 
      i += 1;
    end
  end

  $bm.report "ruby: 10m def foo(a,b,c)" do
    invoke_foo3
  end
  
  def invoke_foo4
    i = 0;
    while i < 100000
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); foo4(nil, nil, nil, nil); 
      i += 1;
    end
  end

  $bm.report "ruby: 10m def foo(a,b,c,d)" do
    invoke_foo4
  end
  
  $bm.report "ruby: 10m def foo() with __send__" do
    a = []; 
    i = 0;
    while i < 100000
      __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); 
      __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); 
      __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); 
      __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); 
      __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); 
      __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); 
      __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); 
      __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); 
      __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); 
      __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); __send__(:foo); 
      i += 1;
    end
  end

  $bm.report "ruby: 10m def opt(a=1) with opt()" do
    a = [];
    i = 0;
    while i < 100000
      optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix;
      optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix;
      optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix;
      optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix;
      optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix;
      optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix;
      optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix;
      optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix;
      optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix;
      optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix; optfix;
      i += 1;
    end
  end

  $bm.report "ruby: 10m def opt(a=1) with opt(nil)" do
    a = [];
    i = 0;
    while i < 100000
      optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil);
      optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil);
      optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil);
      optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil);
      optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil);
      optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil);
      optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil);
      optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil);
      optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil);
      optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil); optfix(nil);
      i += 1;
    end
  end

  $bm.report "ruby: 10m def opt(a=[]) with opt()" do
    a = [];
    i = 0;
    while i < 100000
      optary; optary; optary; optary; optary; optary; optary; optary; optary; optary;
      optary; optary; optary; optary; optary; optary; optary; optary; optary; optary;
      optary; optary; optary; optary; optary; optary; optary; optary; optary; optary;
      optary; optary; optary; optary; optary; optary; optary; optary; optary; optary;
      optary; optary; optary; optary; optary; optary; optary; optary; optary; optary;
      optary; optary; optary; optary; optary; optary; optary; optary; optary; optary;
      optary; optary; optary; optary; optary; optary; optary; optary; optary; optary;
      optary; optary; optary; optary; optary; optary; optary; optary; optary; optary;
      optary; optary; optary; optary; optary; optary; optary; optary; optary; optary;
      optary; optary; optary; optary; optary; optary; optary; optary; optary; optary;
      i += 1;
    end
  end

  $bm.report "ruby: 10m def opt(a=[]) with opt(nil)" do
    a = [];
    i = 0;
    while i < 100000
      optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil);
      optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil);
      optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil);
      optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil);
      optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil);
      optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil);
      optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil);
      optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil);
      optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil);
      optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil); optary(nil);
      i += 1;
    end
  end

  $bm.report "ruby: 10m def opt(a={}) with opt()" do
    a = []; 
    i = 0;
    while i < 100000
      opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash;
      opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash;
      opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash;
      opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash;
      opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash;
      opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash;
      opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash;
      opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash;
      opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash;
      opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash; opthash;
      i += 1;
    end
  end

  $bm.report "ruby: 10m def opt(a={}) with opt(nil)" do
    a = []; 
    i = 0;
    while i < 100000
      opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil);
      opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil);
      opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil);
      opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil);
      opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil);
      opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil);
      opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil);
      opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil);
      opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil);
      opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil); opthash(nil);
      i += 1;
    end
  end

  $bm.report "ruby: 10m def quux(&b) with quux()" do
    a = []; 
    i = 0;
    while i < 100000
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux;
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux;
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux;
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux;
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux;
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux;
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux;
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux;
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux;
      quux; quux; quux; quux; quux; quux; quux; quux; quux; quux;
      i += 1;
    end
  end

  $bm.report "ruby: 10m def quux(&b) with quux(){}" do
    a = []; 
    i = 0;
    while i < 10_000
      quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; 
      quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; 
      quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; 
      quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; 
      quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; 
      quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; 
      quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; 
      quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; 
      quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; 
      quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; quux{}; 
      i += 1;
    end
  end
  
  $bm.report "ruby: 10m define_method :bar {}" do
    a = 0
    while a < 100000
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      bar; bar; bar; bar; bar; bar; bar; bar; bar; bar
      a += 1
    end
  end

  $bm.report "ruby: 10m define_method :bar {|a|}" do
    a = 0
    while a < 100000
      bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1)
      bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1)
      bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1)
      bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1)
      bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1)
      bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1)
      bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1)
      bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1)
      bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1)
      bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1); bar1(1)
      a += 1
    end
  end

  $bm.report "ruby: 10m define_method :bar {|a,b|}" do
    a = 0
    while a < 100000
      bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2)
      bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2)
      bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2)
      bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2)
      bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2)
      bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2)
      bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2)
      bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2)
      bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2)
      bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2); bar2(1,2)
      a += 1
    end
  end

  $bm.report "ruby: 10m define_method :bar {|*a|}" do
    a = 0
    while a < 100000
      bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3)
      bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3)
      bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3)
      bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3)
      bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3)
      bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3)
      bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3)
      bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3)
      bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3)
      bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3); bars(1,2,3)
      a += 1
    end
  end

  $bm = oldbm
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_method_dispatch(bm)} }
end
