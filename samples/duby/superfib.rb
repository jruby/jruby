class Foo
  def fib(n)
    {n => java.lang.Integer::TYPE, :return => java.lang.Integer::TYPE}
    if (n < 2)
      n
    else
      fib(n - 2) + fib(n - 1)
    end
  end
end
