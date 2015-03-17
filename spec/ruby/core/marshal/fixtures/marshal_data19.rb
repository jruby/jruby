module MarshalSpec
  class BasicObjectSubWithRespondToFalse < BasicObject
    def respond_to?(method_name, include_all=false)
      false
    end
  end
end
