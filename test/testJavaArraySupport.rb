require 'test/minirunit'
test_check "Extended Java Array Support"

=begin
  include Java
  
  ruby_array = [1,2,3,4,5,6,7,8,9,0]
  java_array = nil

  # test basic array creation/conversion by to_java
  test_no_exception { java_array = ruby_array.to_java }
  test_ok(java_array.kind_of?(ArrayJavaProxy))
  test_equal(10,java_array.length)
  test_equal("[Ljava.lang.Object;",java_array.java_class.name)
  test_equal(ruby_array,java_array.to_a)
  

  # test created array types match requested types
  
  # built-in type symbols
  type_map = {
    :boolean => "[Z",
    :byte => "[B",
    :char => "[C",
    :short => "[S",
    :int => "[I",
    :long => "[J",
    :float => "[F",
    :double => "[D",
    :Boolean => "[Ljava.lang.Boolean;",
    :Byte => "[Ljava.lang.Byte;",
    :Char => "[Ljava.lang.Character;",
    :Character => "[Ljava.lang.Character;",
    :Short => "[Ljava.lang.Short;",
    :Int => "[Ljava.lang.Integer;",
    :Integer => "[Ljava.lang.Integer;",
    :Long => "[Ljava.lang.Long;",
    :Float => "[Ljava.lang.Float;",
    :Double => "[Ljava.lang.Double;",
    :string => "[Ljava.lang.String;",
    :String => "[Ljava.lang.String;",
    :object => "[Ljava.lang.Object;",
    :Object => "[Ljava.lang.Object;",
    :big_int => "[Ljava.math.BigInteger;",
    :big_integer => "[Ljava.math.BigInteger;",
    :BigInteger => "[Ljava.math.BigInteger;",
    :big_decimal => "[Ljava.math.BigDecimal;",
    :BigDecimal => "[Ljava.math.BigDecimal;",
    
  }
  type_map.each do |type_name, java_type|
    test_no_exception { java_array = ruby_array.to_java(type_name) }
    test_equal(java_type,java_array.java_class.name)
    test_equal(ruby_array.length,java_array.length)
    # compare converted values, except booleans and bigdecimal
    unless type_name == :boolean || type_name == :Boolean || type_name == :big_decimal || type_name == :BigDecimal
      if type_name == :string || type_name == :String
        0.upto(ruby_array.length - 1) do |i|
          test_equal(ruby_array[i], java_array[i].to_i)
        end
      else
        0.upto(ruby_array.length - 1) do |i|
          test_equal(ruby_array[i], java_array[i])
        end
      end
    end
  end
  
  # test arr[n,m]
  int_array = ruby_array.to_java :int
  test_equal(int_array[0,2].to_a,[1,2])
  test_equal(int_array[-2,2].to_a,[9,0])
  test_equal(int_array[-1,10].to_a,[0])
  test_equal(int_array[10,1].to_a,[])
  test_equal(int_array[11,1],nil)

  # test arr[bad_index] still fails
  test_exception(ArgumentError) { int_array[-1] }
  test_exception(ArgumentError) { int_array[999] }

  # test arr.at(n)
  test_equal(int_array.at(-1),0)
  test_equal(int_array.at(999),nil)
  
  # test arr[n..m]
  string_array = ruby_array.to_java :string
  test_equal(string_array[0..2].to_a,["1","2","3"])
  test_equal(string_array[-2..-1].to_a,["9","0"])
  test_equal(string_array[9..10].to_a,["0"])
  test_equal(string_array[10..11].to_a,[])
  test_equal(string_array[11..12],nil)
  
  # test java array + java array
  added = int_array + int_array
  test_equal(20,added.length)
  test_equal("[I",added.java_class.name)
  test_equal(added.to_a,[1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0])

  added = int_array + string_array
  test_equal(20,added.length)
  test_equal("[I",added.java_class.name)
  test_equal(added.to_a,[1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0])

  added = string_array + int_array
  test_equal(20,added.length)
  test_equal("[Ljava.lang.String;",added.java_class.name)
  test_equal(added.to_a,["1","2","3","4","5","6","7","8","9","0","1","2","3","4","5","6","7","8","9","0"])
  
  # test java array + ruby array
  added = int_array + ruby_array
  test_ok(added.kind_of?(ArrayJavaProxy))
  test_equal(20,added.length)
  test_equal("[I",added.java_class.name)
  test_equal(added.to_a,[1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0])

  added = string_array + ruby_array
  test_ok(added.kind_of?(ArrayJavaProxy))
  test_equal(20,added.length)
  test_equal("[Ljava.lang.String;",added.java_class.name)
  test_equal(added.to_a,["1","2","3","4","5","6","7","8","9","0","1","2","3","4","5","6","7","8","9","0"])
  
  # test ruby array + java array
  added = ruby_array + int_array
  test_ok(added.kind_of?(Array))
  test_equal(20,added.length)
  test_equal(added,[1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0])

  added = ruby_array + string_array
  test_ok(added.kind_of?(Array))
  test_equal(20,added.length)
  test_equal(added,[1,2,3,4,5,6,7,8,9,0,"1","2","3","4","5","6","7","8","9","0"])

  # test add and index
  test_equal("867-5309",(string_array[-3,1]+int_array[5...7]+['-']+ruby_array[4,1]+
             string_array[2..2]+int_array[-1,1]+ruby_array[-2..-2]).to_a.join)  
=end
