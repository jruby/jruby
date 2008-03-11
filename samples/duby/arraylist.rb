import "java.util.ArrayList"

class ArrayThing
  import "java.util.List"
  
  def foo
    {:return => :List}
    
    ArrayList.new(1)
  end
  
  def bar(str)
    {str => :string}
    
    list = foo
    
    list.add(str)
    
    puts(list)
  end
end