# coding: utf-8
# frozen_string_literal: false

#--
# additions to class String for Unicode normalization
#++
class String
  def unicode_normalize(form = :nfc)
    require 'unicode_normalize/normalize.rb'
    String.send(:define_method, :unicode_normalize, ->(form = :nfc) { UnicodeNormalize.normalize(self, form) } )
    UnicodeNormalize.normalize(self, form)
  end

  def unicode_normalize!(form = :nfc)
    require 'unicode_normalize/normalize.rb'
    String.send(:define_method, :unicode_normalize!, ->(form = :nfc) { replace(unicode_normalize(form)) } )
    replace(unicode_normalize(form))
  end


  def unicode_normalized?(form = :nfc)
    require 'unicode_normalize/normalize.rb'
    String.send(:define_method, :unicode_normalized?, ->(form = :nfc) { UnicodeNormalize.normalized?(self, form) } )
    UnicodeNormalize.normalized?(self, form)
  end
end

