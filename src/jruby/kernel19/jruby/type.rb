module JRuby
  module Type
    def self.convert_to_str(obj)
      unless obj.respond_to? :to_str
        raise TypeError, "cannot convert #{obj.class} into String"
      end
      obj = obj.to_str
    end
  end
end