fails:Net::FTP#status sends the STAT command to the server
fails:Net::FTP#status returns the received information
fails:Net::FTP#status does not raise an error when the response code is 212
fails:Net::FTP#status does not raise an error when the response code is 213
fails:Net::FTP#status raises a Net::FTPPermError when the response code is 500
fails:Net::FTP#status raises a Net::FTPPermError when the response code is 501
fails:Net::FTP#status raises a Net::FTPPermError when the response code is 502
fails:Net::FTP#status raises a Net::FTPTempError when the response code is 421
fails:Net::FTP#status raises a Net::FTPPermError when the response code is 530
