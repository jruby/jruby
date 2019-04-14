# -*- encoding: us-ascii -*-

require_relative '../../../spec_helper'
require_relative 'shared/select'

describe "Enumerator::Lazy#select" do
  it_behaves_like :enumerator_lazy_select, :select

  it "doesn't pre-evaluate the next element" do
    eval_count = 0
    enum = %w[Text1 Text2 Text3].lazy.select do
      eval_count += 1
      true
    end

    lambda {
      enum.next
    }.should change { eval_count }.from(0).to(1)
  end

  it "doesn't over-evaluate when peeked" do
    eval_count = 0
    enum = %w[Text1 Text2 Text3].lazy.select do
      eval_count += 1
      true
    end

    lambda {
      enum.peek
      enum.peek
    }.should change { eval_count }.from(0).to(1)
  end

  it "doesn't re-evaluate after peek" do
    eval_count = 0
    enum = %w[Text1 Text2 Text3].lazy.select do
      eval_count += 1
      true
    end

    lambda {
      enum.peek
      enum.next
    }.should change { eval_count }.from(0).to(1)
  end
end


