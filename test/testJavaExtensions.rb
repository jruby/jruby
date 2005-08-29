require 'test/minirunit'
test_check "Java Extensions Support"

if defined? Java

  require 'java'

  module JavaCollections
    include_class 'java.util.HashMap'
    include_class "java.util.ArrayList"
    include_class "java.util.HashSet"
    include_class "java.lang.Short"


    def self.testSet
      set = HashSet.new
      set.add(1)
      set.add(2)
      
      newSet = []
      set.each {|x| newSet << x }
      
      test_ok newSet.include?(1)
      test_ok newSet.include?(2)
    end    

    def self.testComparable
      one = Short.new(1)
      two = Short.new(2)
      three = Short.new(3)
      list = [ three, two, one]
      list = list.sort
      test_equal([one, two, three], list)
    end    
    
    def self.testMap
      map = HashMap.new
      map.put('A','1');
      map.put('C','3');
      
      hash = Hash.new
      map.each {|key, value| 
        hash[key] = value
      }
      test_equal('1', hash['A'])
      test_equal('3', hash['C'])
    end
    
    def self.testList  
      a = ArrayList.new
      
      a << 3
      a << 1
      a << 2
      
      test_ok([1, 2, 3], a.sort)
      test_ok([1], a.select {|e| e >= 1 })
    end
    
    testSet
    testMap
    testList
    testComparable
  end
  
  module JavaExceptions
    include_class 'org.jruby.test.TestHelper' 
    include_class 'java.lang.RuntimeException' 
    include_class 'java.lang.NullPointerException' 
    include_class 'org.jruby.test.TestHelper$TestHelperException' do |p,c| "THException"; end
    begin
      TestHelper.throwTestHelperException
    rescue THException => e
    end  
  
    begin
      TestHelper.throwTestHelperException
    rescue NullPointerException => e
      test_fail("Should not rescue")
    rescue THException => e
    end  
  
    begin
      TestHelper.throwTestHelperException
    rescue RuntimeException => e
    end  
  
    begin
      TestHelper.throwTestHelperException
    rescue NativeException => e
    end  
  end
  
  module JavaBeans
    BLUE = "blue"
    GREEN = "green"
    
    include_class 'org.jruby.javasupport.test.Color'
      # Java bean convention properties as attributes
    color = Color.new(GREEN)

    test_ok !color.isDark
    color.dark = true
    test_ok color.dark
    test_ok color.dark?


    test_equal GREEN, color.color

    color.color = BLUE
    test_equal BLUE, color.color
    
  
  end
end
