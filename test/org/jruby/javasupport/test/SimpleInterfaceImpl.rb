require 'java'

include_class 'org.jruby.javasupport.test.SimpleInterface'

class SimpleInterfaceImpl < SimpleInterface
  def initialize
  	super
  	@list = [1,2,3]
  	@empty_list = []
  	@nested_list = [[1,2],[3,4],[5,6]]
  	@nil_list = nil
  	@map = {'A'=>1, 'B' =>2}
  end
  
  def getList; @list; end
  def getEmptyList; @empty_list; end
  def getNestedList; @nested_list; end
  def getNilList; @nil_list; end
  def getMap; @map; end
  def setNilList(list); @nil_list = list; end
  
  def isNilListNil(); @nil_list == nil; end
  
  def modifyNestedList; @nested_list[0] = "FOO"; end
end