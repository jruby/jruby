class Foo
  import 'java.lang.String'
  import 'java.lang.Integer'
  def self.main(args)
    {args => String[]}
    
    puts fib(Integer.parseInt(args[0]))
  end
  
  def self.fib(n)
    {n => :int, :return => :int}
    if (n < 2)
      n
    else
      fib(n - 2) + fib(n - 1)
    end
  end

  def self.fib_iter(n)
    {n => :int, :return => :int}
    a = 0
    b = 1
    i = n
    while i > 0
      c = b
      b = b + a
      a = c
      i = i - 1
    end
    a
  end
end
