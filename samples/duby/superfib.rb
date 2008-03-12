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
end
