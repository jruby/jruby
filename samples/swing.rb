Java.import "javax.swing"

frame = JFrame.new("HelloWorldSwing")
label = JLabel.new("Hello World")

frame.getContentPane().add(label)

frame.setDefaultCloseOperation(JFrame::EXIT_ON_CLOSE)
frame.pack();
frame.setVisible(true);
