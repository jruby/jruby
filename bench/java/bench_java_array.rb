require 'perfer'


Perfer.session 'Java array operations' do |s|
  s.iterate("access ruby array") do |n|
    a = [1,2,3]
    n.times do
      a[0]; a[0]; a[0]; a[0]; a[0]
      a[0]; a[0]; a[0]; a[0]; a[0]
    end
  end

  s.iterate("access java array") do |n|
    a = [1,2,3].to_java :int
    n.times do
      a[0]; a[0]; a[0]; a[0]; a[0]
      a[0]; a[0]; a[0]; a[0]; a[0]
    end
  end

  s.iterate("construct ruby array") do |n|
    n.times do
      a = []; a = []; a = []; a = []; a = []
      a = []; a = []; a = []; a = []; a = []
    end
  end

  s.iterate("construct java array slow") do |n|
    int = Java::int
    n.times do
      int[0].new; int[0].new; int[0].new; int[0].new
      int[0].new; int[0].new; int[0].new; int[0].new
    end
  end

  s.iterate("construct java array medium") do |n|
    int_ary = Java::int[0]
    n.times do
      int_ary.new; int_ary.new; int_ary.new; int_ary.new
      int_ary.new; int_ary.new; int_ary.new; int_ary.new
    end
  end

  if Java::int.respond_to? :new_array
    s.iterate("construct java array new_array") do |n|
      int_ary = Java::int[0]
      n.times do
	int_ary.new; int_ary.new; int_ary.new; int_ary.new
	int_ary.new; int_ary.new; int_ary.new; int_ary.new
      end
    end
  end
end
