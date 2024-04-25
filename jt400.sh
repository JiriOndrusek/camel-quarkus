for i in {1..2}
do
    mvn clean test -f integration-tests/jt400 -Dquarkus.http.test-port=808$i | tee jt400_$i.log &
done