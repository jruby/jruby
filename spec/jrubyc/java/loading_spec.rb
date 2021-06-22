require_relative '../spec_helper'

describe "JRuby::Compiler.compile_argv" do

  def compile_files(files)
    JRuby::Compiler.compile_argv(files)
  end

  FILES_DIR = File.expand_path('files', File.dirname(__FILE__))

  before(:all) do
    argv = [ '--dir', FILES_DIR, '--target', FILES_DIR ]
    rb_files = Dir.glob(File.join(FILES_DIR, '*.rb'))
    argv.push *rb_files

    puts argv.inspect if $VERBOSE

    JRuby::Compiler.compile_argv argv
  end

  after(:all) do
    Dir.glob(File.join(FILES_DIR, '*.class')).each { |f| File.delete(f) }
  end

  it "loads double_rescue.class" do
    load File.join(FILES_DIR, 'double_rescue.class')

    expect( Object.const_defined?(:DoubleRescue) ).to be true
    DoubleRescue.new._call

    expect( DoubleRescue.re_raise_return ).to be_a LoadError
    expect { DoubleRescue.re_raise }.to raise_error LoadError
  end

  it "loads for.class" do
    load File.join(FILES_DIR, 'for.class')
    expect( $for_result ).to eql 3
    expect( $for_nested_result ).to eql 28
  end

  it "loads sample_block.class" do
    load File.join(FILES_DIR, 'sample_block.class')

    expect( Object.const_defined?(:SampleBlock) ).to be true
    expect( SampleBlock.class_variable_get :@@func ).to eql '11'
  end

  it "deserializes symbol_proc.class" do
    load File.join(FILES_DIR, 'symbol_proc.class')

    expect( $symbol_proc_result ).to_not be nil
    expect( $symbol_proc_result ).to eql [ 1, 2, 3 ]
  end

  it "can load all number operands" do
    load File.join(FILES_DIR, 'operands.class')

    expect( $numbers_result ).to eql [1r, 1i, 1, 1.0]
  end

  it "compiles hashy_kwargs.class correctly" do
    load File.join(FILES_DIR, 'hashy_kwargs.class')

    expect( Object.const_defined?(:HashyKwargs) ).to be true
    klass = Class.new { include HashyKwargs }
    res = klass.new.generic('0', 111, arg: 1) { 'block' }

    expect( res[0] ).to eql [ '0', 111 ]
    expect( res[1] ).to eql({ :arg => 1 })
    expect( res[2].call ).to eql 'block'
    expect( res.size ).to be 3

    expect( HashyKwargs.kwargs1(sec: '2') ).to eql 1
    expect( HashyKwargs.kwargs1(sym: false) ).to eql 2

    res = HashyKwargs.kwargs2(foo: :bar, baz: 0, req: true)
    expect( res ).to eql [ true, { :foo => :bar, :baz => 0 }]
  end

end
