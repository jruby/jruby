class Person < RedisOrm::Base
  property :name, String
  property :email, String
  index :email

  timestamps

  after_create :created

  def created
    puts format '%s created', self
  end

  def as_json(options = {})
    { id: id, name: name, email: email }
  end

  def to_model
    self
  end
end
