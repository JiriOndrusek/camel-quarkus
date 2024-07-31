openssl pkcs12 -in localhost.jks  -nodes -nocerts -out localhost-key.pem
openssl pkcs12 -export -out combined.p12 -inkey localhost-key.pem -in localhost.pem -certfile splunkca.pem
openssl pkcs12 -in combined.p12 -out combined.pem -nodes