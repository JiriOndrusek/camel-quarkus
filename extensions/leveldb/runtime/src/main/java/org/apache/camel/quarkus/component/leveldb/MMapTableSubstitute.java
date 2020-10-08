package org.apache.camel.quarkus.component.leveldb;

import java.util.concurrent.Callable;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.iq80.leveldb.table.MMapTable;

@TargetClass(value = MMapTable.class)
final class MMapTableSubstitute {

    @Substitute
    public Callable<?> closer() {
        return null;
    }

    //    @Alias
    //    private final LoadingCache<Long, TableCache.TableAndFile> cache;
    //
    //
    //    public TableCacheSubstitute(final File databaseDir, int tableCacheSize, final UserComparator userComparator, final boolean verifyChecksums)
    //        {
    //            requireNonNull(databaseDir, "databaseName is null");
    //
    //            cache = CacheBuilder.newBuilder()
    //                    .maximumSize(tableCacheSize)
    //                    .build(new CacheLoader<Long, org.iq80.leveldb.impl.TableCache.TableAndFile>()
    //                    {
    //                        @Override
    //                        public org.iq80.leveldb.impl.TableCache.TableAndFile load(Long fileNumber)
    //                                throws IOException
    //                        {
    //                            return new TableCache.TableAndFile(databaseDir, fileNumber, userComparator, verifyChecksums);
    //                        }
    //                    });
    //        }
}
