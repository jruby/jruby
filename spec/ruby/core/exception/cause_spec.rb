require_relative '../../spec_helper'

describe "Exception#cause" do
  it "returns the active exception when an exception is raised" do
    begin
      raise Exception, "the cause"
    rescue Exception => cause
      -> {
        raise RuntimeError, "the consequence"
      }.should raise_error(RuntimeError, "the consequence", cause:)
    end
  end

  it "is set for user errors caused by internal errors" do
    begin
      1 / 0
    rescue => cause
      -> { raise "foo" }.should raise_error(RuntimeError, cause:)
    end
  end

  it "is set for internal errors caused by user errors" do
    cause = RuntimeError.new "cause"
    begin
      raise cause
    rescue
      -> { 1 / 0 }.should raise_error(ZeroDivisionError, cause:)
    end
  end

  it "is not set to the exception itself when it is re-raised" do
    begin
      raise RuntimeError
    rescue RuntimeError => e
      -> { raise e }.should raise_error(RuntimeError, cause: nil)
    end
  end
end
