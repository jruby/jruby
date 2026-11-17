describe "A module with a prepend" do
  describe "when its method table is modified" do
    it "invalidates previously cached calls" do
      module A
        def foo
          "foo"
        end
      end
      class X
        include A
      end
      A.prepend(Module.new)
      call_foo = ->{ X.new.foo }
      expect(call_foo.()).to eq "foo"
      module A
        def foo
          "foo2"
        end
      end
      expect(call_foo.()).to eq "foo2"
    end
  end
end