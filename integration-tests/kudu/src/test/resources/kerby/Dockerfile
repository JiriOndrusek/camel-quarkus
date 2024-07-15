FROM apache/kudu:1.17.0
COPY --chown=kudu:kudu kudu-master /home/kudu/kudu-master
COPY --chown=kudu:kudu kudu-tserver /home/kudu/kudu-tserver
#COPY --chown=kudu:kudu krb5.conf /etc/krb5.conf