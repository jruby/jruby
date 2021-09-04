require 'virtus'
require 'zip'

class Hello
  include Virtus

  attribute :name, String

  def say
    z = Zip::ZipFile.new( name, Zip::ZipFile::CREATE )

    "hello #{name}\n" +
      "zip file name: #{z.name}"
  end
end
