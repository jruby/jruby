# -*- encoding: utf-8 -*-
require 'stringio'
require File.expand_path('../shared/getc', __FILE__)

describe "StringIO#getc" do
  it_behaves_like :stringio_getc, :getc

  it "returns the charactor at the current position" do
    io = StringIO.new("example")

    io.send(@method).should == ?e
    io.send(@method).should == ?x
    io.send(@method).should == ?a
  end

  with_feature :encoding do
    it "increments #pos by the byte size of the character in multibyte strings" do
      io = StringIO.new("föóbar")

      io.send(@method); io.pos.should == 1 # "f" has byte size 1
      io.send(@method); io.pos.should == 3 # "ö" has byte size 2
      io.send(@method); io.pos.should == 5 # "ó" has byte size 2
      io.send(@method); io.pos.should == 6 # "b" has byte size 1
    end
  end
end

describe "StringIO#getc when self is not readable" do
  it_behaves_like :stringio_getc_not_readable, :getc
end
