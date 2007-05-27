# The code is ugly because it must be kept as straightforward
# as possible, do not blame me.

# JRuby can run rspec, so we just require it here
#require 'mini_rspec'

class Object
  # Provide the method name and a hash with any of the following
  #   :with       => Array of arguments or :any (default)
  #   :block      => Whether block is present or :any (default)
  #   :count      => Number of times invoked, default once
  #   :returning  => Object to return
  def should_receive(sym, info = {:with => :any, :block => :any, :count => 1})
    meta = class << self; self; end
    
    if meta.instance_methods.include?(sym)
      meta.send :alias_method, :"__ms_#{sym}", sym.to_sym
      Mock.set_objects self, sym, :single_overridden 
    else 
      Mock.set_objects self, sym, :single_new 
    end

    meta.class_eval <<-END
      def #{sym}(*args, &block)
        Mock.report self, :#{sym}, *args, &block
      end
    END
    
    info[:with]   = info[:with] || :any
    info[:block]  = info[:block] || :any
    info[:count]  = info[:count] || 1

    Mock.set_expect self, sym, info 
  end

  # Same as should_receive except that :count is 0
  def should_not_receive(sym, info = {:with => :any, :block => :any, :count => 0})
    info[:count] = 0
    should_receive sym, info
  end
end 


module Mock
  def self.reset()
    $__ms_expects = {}
    $__ms_objects = []
  end

  def self.set_expect(obj, sym, info)
    $__ms_expects[[obj, sym]] = info
  end

  def self.set_objects(obj, sym, type = nil)
    $__ms_objects << [obj, sym, type]
  end

  # Verify to correct number of calls
  def self.verify()
    $__ms_expects.each {|k, info|
      obj, sym = k[0], k[1]

      if info[:count] != :never 
        if info[:count] > 0
          raise Exception.new("Method #{sym} with #{info[:with].inspect} and block #{info[:block].inspect} called too FEW times on object #{obj.inspect}")
        end
      end
    }
  end

  # Clean up any methods we set up
  def self.cleanup()
    $__ms_objects.each {|info|
      obj, sym, type = info[0], info[1], info[2]

      hidden_name = "__ms_" + sym.to_s

      # Revert the object back to original if possible
      case type
        when :all_instances
          next
        
        when :single_new
          meta = class << obj; self; end
#          meta.send :remove_method, sym.to_sym
              
        when :single_overridden
          meta = class << obj; self; end
          meta.class_eval "alias #{sym} #{hidden_name}"
      end
    }
  end  

  # Invoked by a replaced method in an object somewhere.
  # Verifies that the method is called as expected with
  # the exception that calling the method too few times
  # is not detected until #verify_expects! gets called
  # which by default happens at the end of a #specify
  def self.report(obj, sym, *args, &block)
    info = $__ms_expects[[obj, sym]]

    unless info[:with] == :any
      unless info[:with] == args
        return
      end
    end

    unless info[:block] == :any
      if block
        unless info[:block]
          return
        end
      end
    end

    if info[:count] == :never
      raise Exception.new("Method #{sym} with #{info[:with].inspect} and block #{info[:block].inspect} should NOT be called on object #{obj.inspect}")
    end
    
    info[:count] = info[:count] - 1

    if info[:count] < 0
      raise Exception.new("Method #{sym} with #{info[:with].inspect} and block #{info[:block].inspect} called too MANY times on object #{obj.inspect}")
    end

    return info[:returning]
  end
end

# Start up
Mock.reset
