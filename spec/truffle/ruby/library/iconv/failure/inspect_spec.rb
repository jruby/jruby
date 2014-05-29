require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes.rb', __FILE__)

ruby_version_is ''...'2.0' do 
  describe "Iconv::Failure#inspect" do
    it "includes information on the exception class name, #succes and #failed" do
      lambda {
        begin
          Iconv.open "utf-8", "utf-8" do |conv|
            conv.iconv "testing string \x80 until an error occurred"
          end
        rescue Iconv::Failure => e
          @ex = e
          raise e
        end
      }.should raise_error(Iconv::Failure)
      inspection = @ex.inspect
      inspection.should include(@ex.class.to_s)
      inspection.should include(@ex.success.inspect)
      inspection.should include(@ex.failed.inspect)
    end
  end
end
