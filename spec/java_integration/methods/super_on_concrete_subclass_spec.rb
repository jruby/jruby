require File.dirname(__FILE__) + "/../spec_helper"

# See GH-6718
describe "A Java method overidden by a Ruby subclass" do
  it "can dispatch to the super method" do
    class Parent < java.lang.Object
      def toString
        "parent #{super}"
      end
    end

    expect(Parent.new.toString).to match(/^parent/)

    class Child < Parent
    end

    expect(Child.new.toString).to match(/^parent/)
  end

  describe "and further overridden by a second Ruby subclass" do
    it "can dispatch to both super methods" do
      class Parent < java.lang.Object
        def toString
          "parent #{super}"
        end
      end

      expect(Parent.new.toString).to match(/^parent/)

      class Child < Parent
        def toString
          "child #{super}"
        end
      end

      expect(Child.new.toString).to match(/^child parent/)
    end
  end
end