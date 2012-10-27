module Mocha
  
  class Central
  
    attr_accessor :stubba_methods
  
    def initialize
      self.stubba_methods = []
    end
   
    def stub(method)
      unless stubba_methods.include?(method)
        method.stub 
        stubba_methods.push(method)
      end
    end
    
    def unstub(method)
      if stubba_methods.include?(method)
        method.unstub
        stubba_methods.delete(method)
      end
    end
    
    def unstub_all
      while stubba_methods.any? do
        unstub(stubba_methods.first)
      end
    end

  end

end