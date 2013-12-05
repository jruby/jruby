# encoding: utf-8

describe "An encoded date format" do
  it "properly preserves the pattern's encoding" do
    result = Time.at(100).strftime('Процесс завершен за %S')
    result.should == "Процесс завершен за 40"
  end
end
