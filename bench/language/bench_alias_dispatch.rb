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

  oldbm = $bm
  $bm = bm
  def _foo
    self
  end

  def _foo1(arg)
    self
  end

  def _foo2(arg, arg2)
    self
  end

  def _foo3(arg, arg2, arg3)
    self
  end

  def _foo4(arg, arg2, arg3, arg4)
    self
  end

  def _foos(*args)
    self
  end
  
  def _baz(opts = { })
    self
  end

  def _quux(&block)
    self
  end

  alias foo _foo
  alias foo1 _foo1
  alias foo2 _foo2
  alias foo3 _foo3
  alias foo4 _foo4
  alias foos _foos
  alias baz _baz
  alias quux _quux

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
  
  $bm = oldbm
end

if $0 == __FILE__
  (ARGV[0] || 5).to_i.times { Benchmark.bm(40) {|bm| bench_method_dispatch(bm)} }
end
