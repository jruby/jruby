class PeopleController < ApplicationController

  PLATFORM = if defined? Truffle
               Truffle.graal? ? :graal : :truffle
             else
               :jruby
             end

  PEOPLE = [
      { name: 'John Doe', email: 'jd@example.com' }
  ]

  skip_before_filter :verify_authenticity_token

  def index
    @people = PEOPLE
    respond_to do |format|
      format.json { render :json => @people }
      format.html
    end
  end

  def create
    PEOPLE << person = { name: params[:name], email: params[:email] }
    respond_to do |format|
      format.json { render :json => person }
      format.html { redirect_to action: :index }
    end
  end

  def platform
    respond_to do |format|
      format.json { render :json => { platform: PLATFORM } }
    end
  end

end
