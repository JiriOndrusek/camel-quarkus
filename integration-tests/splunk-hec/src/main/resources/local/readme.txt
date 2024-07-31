add sslContextParameters to splunk hec and if present, set ssl context to http5client
negative scenario should use wrong_certificate

openssl pkcs12 -in localhost.jks  -nodes -nocerts -out localhost-key.pem
openssl pkcs12 -export -out combined.p12 -inkey localhost-key.pem -in localhost.pem -certfile splunkca.pem
openssl pkcs12 -in combined.p12 -out combined.pem -nodes



openssl s_client -connect localhost:32825 -CAfile cacert.pem