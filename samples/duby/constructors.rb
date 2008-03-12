class MyFoo
  def initialize(a, b)
    {a => :string, b => :int}
    
    @c = a.substring b
  end
  
  def foo
    puts @c
  end
end