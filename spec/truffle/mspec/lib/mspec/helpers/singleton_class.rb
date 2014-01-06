class Object
  unless method_defined? :singleton_class
    def singleton_class
      class << self; self; end
    end
  end
end
