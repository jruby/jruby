require 'java'
require 'fileutils.rb'

java_import org.jruby.ast.Node
java_import org.jruby.ir.IRBuilder
java_import org.jruby.ir.IRManager
java_import org.jruby.ir.IRScope
java_import org.jruby.ir.persistence.IRPeristenceFacade

# Fibbonacci example from http://www.engineyard.com/blog/2012/oss-grant-roundup-jruby-runtime/ is used here
describe IRPeristenceFacade do
  # Pathes
  REFERENCE_DIR = "#{File.expand_path(File.dirname(__FILE__))}/reference"
  REFERENCE_RUBY_FILE_PATH = "#{REFERENCE_DIR}/fib_ruby.rb"
  REFERENCE_IR_FILE_PATH = "#{REFERENCE_DIR}/fib_ir.ir"
  EXPECTED_CREATED_IR_FOLDER_PATH = "#{REFERENCE_DIR}/ir"
  EXPECTED_CREATED_IR_FILE_PATH = "#{EXPECTED_CREATED_IR_FOLDER_PATH}/fib_ruby.ir"

  # Produce IRScope from .rb file
  before :all do
    is_command_line_script = false
    ast = IRBuilder.buildAST(is_command_line_script, REFERENCE_RUBY_FILE_PATH) 
    manager = IRManager.new
    @scope = IRBuilder.createIRBuilder(manager).buildRoot(ast)
  end

  # Clean up
  after :all do
    if File.exists?(EXPECTED_CREATED_IR_FILE_PATH)
      File.delete(EXPECTED_CREATED_IR_FILE_PATH)
      Dir.delete(EXPECTED_CREATED_IR_FOLDER_PATH)
    end
  end

  describe '.persist' do    
    before :all do
      IRPersistenceFacade.persist(@scope)
    end

    it 'should produce IR as output' do
      output = IRPersistence.persist(@scope)
      output.should == @scope.toStringInstrs
    end
    
    it 'should create .ir file in ir directory relatively to rb file' do
      File.exists?(EXPECTED_CREATED_IR_FILE_PATH).should be_true
    end

    it 'should create file identical to the reference one' do
      FileUtils.identical?(REFERENCE_RUBY_FILE_PATH, EXPECTED_CREATED_IR_FILE_PATH).should be_true
    end
  end

  # not quite sure whether we need IRPersistenceFacade.execute method or IRPersistenceFacade.read will be enough
  describe '.execute' do
    it 'should execute persisted code with expected result'
  end

  describe '.read' do
    it 'should read IR from disk without loss of information'
  end

end
