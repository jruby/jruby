# -*- encoding: utf-8 -*-
require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/chars', __FILE__)

ruby_version_is '1.8.7' do
  describe "IO#chars" do
    it_behaves_like :io_chars, :chars
  end

  describe "IO#chars" do
    it_behaves_like :io_chars_empty, :chars
  end
end
