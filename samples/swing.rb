module Swing
  include_package "javax.swing"
end

frame = Swing::JFrame.new()
label = Swing::JLabel.new("Hello World")

frame.getContentPane().add(label)
# frame.setDefaultCloseOperation(Swing::JFrame::EXIT_ON_CLOSE)
frame.pack();
frame.setVisible(true);
