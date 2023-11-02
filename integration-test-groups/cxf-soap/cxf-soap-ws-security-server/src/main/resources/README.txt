openssl genrsa -out cxfca.key 2048
openssl req -x509 -new -key cxfca.key -nodes -out cxfca.pem -config cxfca-openssl.cnf -days 3650 -extensions v3_req
openssl genrsa -out alice.key 2048
openssl req -x509 -new -key alice.key -nodes -out alice.pem -config alice-openssl.cnf -days 3650 -extensions v3_req
openssl genrsa -out bob.key 2048
openssl req -x509 -new -key bob.key -nodes -out bob.pem -config bob-openssl.cnf -days 3650 -extensions v3_req

openssl req -new -subj '//O=apache.org/OU=eng/CN=cxfca' -x509 -key cxfca.key -out cxfca.crt

openssl req -new -subj '/O=apache.org/L=camel-ftp/OU=eng/CN=alice' -key alice.key -out alice.csr
openssl x509 -req -in alice.csr -CA cxfca.pem -CAkey cxfca.key -CAcreateserial -out alice.crt

openssl req -new -subj '/O=apache.org/OU=eng/CN=bob' -key bob.key -out bob.csr
openssl x509 -req -in bob.csr -CA cxfca.pem -CAkey cxfca.key -CAcreateserial -out bob.crt

openssl pkcs12 -export -in alice.crt -inkey alice.key -certfile cxfca.crt -name "alice" -out alice.p12 -passout pass:password -keypbe aes-256-cbc -certpbe aes-256-cbc
openssl pkcs12 -export -in bob.crt -inkey bob.key -certfile cxfca.crt -name "bob" -out bob.p12 -passout pass:password -keypbe aes-256-cbc -certpbe aes-256-cbc

//not sure about following 2 cmds
keytool -import -trustcacerts -alias bob -file bob.crt -keystore alice.p12
keytool -import -trustcacerts -alias alice -file alice.crt -keystore bob.p12