require 'benchmark'

def bench_method_dispatch(bm)
  bm.report "control: 100x x100 local var access" do
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

  bm.report "core class: 100x x100 Fixnum#to_i" do
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
  
  def baz(opts = { })
    self
  end

  def quux(&block)
    self
  end

  $bm.report "ruby method: 100k x100 self.foo" do
    a = []; 
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

  $bm.report "ruby method: 100k x100 self.foos" do
    a = []; 
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

  $bm.report "ruby method: 100k x100 self.foos(1)" do
    a = []; 
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
  
  $bm.report "ruby method: 100k x100 self.foos(4)" do
    a = []; 
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

  $bm.report "ruby method: 100k x100 self.foo1" do
    a = []; 
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

  $bm.report "ruby method: 100k x100 self.foo2" do
    a = []; 
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
  
  $bm.report "ruby method: 100k x100 self.foo3" do
    a = []; 
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
  
  $bm.report "ruby method: 100k x100 self.foo4" do
    a = []; 
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
  
  $bm.report "__send__ method: 100k x100 self.foo" do
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

  $bm.report "ruby method(opt): 100k x100 self.baz" do
    a = []; 
    i = 0;
    while i < 100000
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz; 
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz; 
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz; 
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz; 
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz; 
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz; 
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz; 
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz; 
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz; 
      baz; baz; baz; baz; baz; baz; baz; baz; baz; baz; 
      i += 1;
    end
  end

  $bm.report "ruby method(no opt): 100k x100 self.baz" do
    a = []; 
    i = 0;
    while i < 100000
      baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); 
      baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); 
      baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); 
      baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); 
      baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); 
      baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); 
      baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); 
      baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); 
      baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); 
      baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); baz(nil); 
      i += 1;
    end
  end

  $bm.report "ruby method(block): 100k x100 self.quux" do
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

  $bm.report "ruby method(block{}): 10k x100 self.quux" do
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
  
  $bm.report "define_method method: 100k x100 calls" do
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

  $bm = oldbm
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_method_dispatch(bm)} }
end
