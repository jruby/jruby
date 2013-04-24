The files here are specifically named to correspond with the hash of the remote
certificate. I (headius) do not really understand the process that cert
verification goes through, so I just put these files in the right filename based
on what `truss` told me that MRI's OpenSSL calls were looking for. Both certs
have the content of "Verisign Class 3 Public Primary Certification Authority" as
of the date of this commit. OpenSSL's s_client command looked for the 765 file
and MRI's OpenSSL calls looked for the 415 file. The related tests are in file
openssl/test_integration.rb in test_ca_path_name.

Because this is testing only a single CA cert, if the test's target server
(www.amazon.com) changes their CA these files will need to be updated.