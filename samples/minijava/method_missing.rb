import 'java.lang.Runnable'
import 'java.lang.Thread'

X = new_class(Runnable)
class X
  def method_missing(*args)
    puts 'here'
  end
end

Thread.new(X.new,'mythread'.to_java).start
