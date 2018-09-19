describe "Module#module_function" do
  it "will call method_added before method_singleton_added" do
    module GH4732
      def self.order
        @order ||= []
      end
      
      def self.method_added(name)
        order << [:method_added, name]
      end

      def self.singleton_method_added(name)
        order << [:singleton_method_added, name]
      end

      module_function

      def foo
      end
    end
    
    expect(GH4732.order).to eq([[:singleton_method_added, :singleton_method_added], [:method_added, :foo], [:singleton_method_added, :foo]])
  end
end
