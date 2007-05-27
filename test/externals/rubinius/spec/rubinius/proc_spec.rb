require File.dirname(__FILE__) + '/../spec_helper'

context "A Proc instance created using Proc.given" do
  specify "should be nil if no block given" do
    def answer
      Proc.given.nil?
    end

    answer.should == true
  end

  specify "should reference the same block" do
    def answer(&prc)
      prc.block.object_id == Proc.given.block.object_id
    end

    answer {}.should == true
  end

  specify "should be able to call the block" do
    def answer(&prc)
      a = [prc.nil?, Proc.given.nil?]
      a << (block_given? == !Proc.given.nil?)
      if block_given?
         a << (prc.object_id == Proc.given.object_id)
         a << (prc.block.object_id == Proc.given.block.object_id)
         a << Proc.given.call(21)
      end
      a
    end
      
    answer.should == [true, true, true] 
    answer { |n| n * 2}.should == [false, false, true, false, true, 42]
  end

  specify "using a MethodContext should do the mind trick" do
    @message = ''
    def stormtrooper
      yield "Let me see your identification."
      obiwan { |reply| @message << "Obi-Wan: #{reply}" }
    end
    
    def obiwan
      yield "[with a small wave of his hand] You don't need to see his identification."
      ctx = MethodContext.current.sender
      Proc.given(ctx).call("We don't need to see his identification.")
    end
    
    message = "Stormtrooper: Let me see your identification." \
              "Obi-Wan: [with a small wave of his hand] You don't need to see his identification." \
              "Stormtrooper: We don't need to see his identification."

    stormtrooper { |msg| @message <<  "Stormtrooper: #{msg}" }
    @message.should == message
  end
end
