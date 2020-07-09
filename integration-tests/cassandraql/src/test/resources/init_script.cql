CREATE KEYSPACE IF NOT EXISTS test WITH replication = {'class':'SimpleStrategy','replication_factor':'1'};

CREATE TABLE test.employee(
                    id int PRIMARY KEY,
                    name text,
                    address text);

CREATE KEYSPACE IF NOT EXISTS k1 WITH replication = {'class':'SimpleStrategy', 'replication_factor':1};
CREATE TABLE IF NOT EXISTS k1.fruit(store_id text, name text, description text, PRIMARY KEY((store_id), name));