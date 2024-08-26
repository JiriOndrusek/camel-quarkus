https://github.com/coheigea/testcases/blob/master/apache/docker/kerby/kerby-data/conf/krb5.conf


to add a principal in docker:
sh bin/kadmin.sh /kerby-data/conf/ -k /kerby-data/keytabs/admin.keytab -q "addprinc -pw changeit alice@EXAMPLE.COM"

export keytab
sh bin/kadmin.sh /kerby-data/conf/ -k /kerby-data/keytabs/admin.keytab -q "ktadd -k /kerby-data/keytabs/alice.keytab alice"

then copy from docker



verify manually:
export KRB5_CONFIG=/home/jondruse/git/community/camel-quarkus/integration-tests/kudu/target/kerby/krb5.conf2
kinit -k -t principals3.keytab kudu@EXAMPLE.COM
klist