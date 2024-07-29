export key:

openssl pkcs12 -in keystore.p12  -nodes -nocerts -out key.pem

openssl s_client -showcerts -connect localhost:32835

openssl s_client -connect localhost:443 -CAfile splunkca.pem
