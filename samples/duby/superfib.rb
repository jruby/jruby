class Foo
  def fib(n)
    {n => :int, :return => :int}
    if (n < 2)
      n
    else
      fib(n - 2) + fib(n - 1)
    end
  end
end
