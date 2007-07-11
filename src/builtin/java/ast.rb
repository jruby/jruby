class Java::OrgJrubyAst::Node
  def inspect(indent = 0)
    s = ' '*indent + self.class.name.split('::').last

    if self.respond_to?(:name)
      s << " |#{self.name}|"
    end
    if self.respond_to?(:value)
      s << " ==#{self.value.inspect}"
    end

    [:receiver_node, :args_node, :iter_node, :body_node, :next_node].each do |mm|
      if self.respond_to?(mm)
        begin 
          s << "\n#{self.send(mm).inspect(indent+2)}" if self.send(mm)
        rescue
          s << "\n#{' '*(indent+2)}#{self.send(mm).inspect}" if self.send(mm)
        end
      end
    end

    if Java::OrgJrubyAst::ListNode === self
      (0...self.size).each do |n|
        begin
          s << "\n#{self.get(n).inspect(indent+2)}" if self.get(n)
        rescue
          s << "\n#{' '*(indent+2)}#{self.get(n).inspect}" if self.get(n)
        end
      end
    end
    s
  end
  
  def to_yaml_node(out)
    content = []
    content << {'name' => self.name} if self.respond_to?(:name)
    content << {'value' => self.value} if self.respond_to?(:value)
    [:receiver_node, :args_node, :iter_node, :body_node, :next_node].each do |mm|
      if self.respond_to?(mm)
        content << self.send(mm) if self.send(mm)
      end
    end
    if Java::OrgJrubyAst::ListNode === self
      (0...self.size).each do |n|
        content << self.get(n) if self.get(n)
      end
    end
    out.map({ }.taguri, { self.class.name.split('::').last => content }, nil)
  end
end

