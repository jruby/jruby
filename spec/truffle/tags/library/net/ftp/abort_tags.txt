fails:Net::FTP#abort sends the ABOR command to the server
fails:Net::FTP#abort ignores the response
fails:Net::FTP#abort returns the full response
fails:Net::FTP#abort does not raise any error when the response code is 225
fails:Net::FTP#abort does not raise any error when the response code is 226
fails:Net::FTP#abort raises a Net::FTPProtoError when the response code is 500
fails:Net::FTP#abort raises a Net::FTPProtoError when the response code is 501
fails:Net::FTP#abort raises a Net::FTPProtoError when the response code is 502
fails:Net::FTP#abort raises a Net::FTPProtoError when the response code is 421
