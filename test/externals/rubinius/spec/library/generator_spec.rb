require File.dirname(__FILE__) + '/../spec_helper'

describe "Generotar.new" do
  require 'generator'

  it "creates a new generator from an Enumerable object" do
    g = Generator.new(['A', 'B', 'C', 'Z'])
    g.should_not == nil
    g.kind_of?(Generator).should == true
  end
  
  it "creates a new generator from a block" do
    g = Generator.new { |g|
      for i in 'A'..'C'
        g.yield i
      end
      g.yield 'Z'
    }

    g.should_not == nil
    g.kind_of?(Generator).should == true
  end
end

describe "Generator#next?" do
  it "returns false for empty generator" do
    g = Generator.new([])
    g.next?.should == false
  end

  it "returns true for non-empty generator" do
    g = Generator.new([1])
    g.next?.should == true

    g = Generator.new([1, 2])
    g.next?.should == true

    g =Generator.new(['A', 'B', 'C', 'D', 'E', 'F'])
    g.next?.should == true
  end

  it "returns true if the generator has not reached the end yet" do
    g = Generator.new([1, 2])
    g.next
    g.next?.should == true
  end

  it "returns false if the generator has reached the end" do
    g = Generator.new([1, 2])
    g.next
    g.next
    g.next?.should == false
  end
  
  it "returns false if end? returns true" do
    g = Generator.new([1, 2])
    def g.end?; true end
    g.next?.should == false
  end
end

describe "Generator#next" do
  it "raises EOFError on empty generator" do
    should_raise(EOFError) do
      g = Generator.new([])
      g.next
    end
  end

  it "raises EOFError if no elements available" do
    g = Generator.new([1, 2])
    g.next; g.next
    should_raise(EOFError) do
      g.next
    end
  end

  it "raises EOFError if end? returns true" do
    g = Generator.new([1, 2])
    def g.end?; true end
    should_raise(EOFError) do
      g.next
    end
  end
  
  it "returns the element at the current position and moves forward" do
    g = Generator.new([1, 2])
    g.index.should == 0
    g.next.should == 1
    g.index.should == 1
  end
  
  it "subsequent calls should return all elements in proper order" do
    g = Generator.new(['A', 'B', 'C', 'Z'])

    result = []
    while g.next?
      result << g.next
    end
    
    result.should == ['A', 'B', 'C', 'Z']    
  end 
end

describe "Generator#rewind" do
  it "does nothing for empty generator" do
    g = Generator.new([])
    g.index.should == 0
    g.rewind
    g.index.should == 0
  end

  it "rewinds the generator" do
    g = Generator.new([1, 2])
    orig = g.next
    g.index.should == 1
    g.rewind
    g.index.should == 0
    g.next.should == orig
  end

  it "rewinds the previously finished generator" do
    g = Generator.new([1, 2])
    g.next; g.next
    g.rewind
    g.end?.should == false
    g.next?.should == true
    g.next.should == 1
  end
end

describe "Generator#each" do
  it "enumerates the elements" do
    g = Generator.new(['A', 'B', 'C', 'Z'])
    result = []

    g.each { |element|
      result << element
    }

    result.should == ['A', 'B', 'C', 'Z']    
  end

  it "rewinds the generator and only then enumerates the elements" do
    g = Generator.new(['A', 'B', 'C', 'Z'])
    g.next; g.next
    result = []

    g.each { |element|
      result << element
    }

    result.should == ['A', 'B', 'C', 'Z']    
  end
end
