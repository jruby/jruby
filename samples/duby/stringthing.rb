class Foo
  def bar
    {:return => java.lang.String}
    
    'here'
  end
end

class Foo
  # reopening even!
  def baz(a)
    {a => java.lang.String}
    
    b = "foo"
    a = a + bar + b
    puts a
  end
end
