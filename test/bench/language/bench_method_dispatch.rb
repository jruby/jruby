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

  class << self
    define_method(:bar) { }
  end
  
  def baz(opts = { })
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
