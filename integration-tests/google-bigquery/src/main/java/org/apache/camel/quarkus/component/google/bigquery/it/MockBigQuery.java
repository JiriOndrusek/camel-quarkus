package org.apache.camel.quarkus.component.google.bigquery.it;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import com.google.api.gax.paging.Page;
import com.google.cloud.Policy;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Connection;
import com.google.cloud.bigquery.ConnectionSettings;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.DatasetInfo;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobException;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.Model;
import com.google.cloud.bigquery.ModelId;
import com.google.cloud.bigquery.ModelInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryResponse;
import com.google.cloud.bigquery.Routine;
import com.google.cloud.bigquery.RoutineId;
import com.google.cloud.bigquery.RoutineInfo;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableDataWriteChannel;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.bigquery.WriteChannelConfiguration;

@ApplicationScoped
@Named("namedBean")
public class MockBigQuery implements BigQuery {
    @Override
    public Dataset create(DatasetInfo datasetInfo, DatasetOption... options) {
        return null;
    }

    @Override
    public Table create(TableInfo tableInfo, TableOption... options) {
        return null;
    }

    @Override
    public Routine create(RoutineInfo routineInfo, RoutineOption... options) {
        return null;
    }

    @Override
    public Job create(JobInfo jobInfo, JobOption... options) {
        return null;
    }

    @Override
    public Connection createConnection(ConnectionSettings connectionSettings) {
        return null;
    }

    @Override
    public Connection createConnection() {
        return null;
    }

    @Override
    public Dataset getDataset(String datasetId, DatasetOption... options) {
        return null;
    }

    @Override
    public Dataset getDataset(DatasetId datasetId, DatasetOption... options) {
        return null;
    }

    @Override
    public Page<Dataset> listDatasets(DatasetListOption... options) {
        return null;
    }

    @Override
    public Page<Dataset> listDatasets(String projectId, DatasetListOption... options) {
        return null;
    }

    @Override
    public boolean delete(String datasetId, DatasetDeleteOption... options) {
        return false;
    }

    @Override
    public boolean delete(DatasetId datasetId, DatasetDeleteOption... options) {
        return false;
    }

    @Override
    public boolean delete(String datasetId, String tableId) {
        return false;
    }

    @Override
    public boolean delete(TableId tableId) {
        return false;
    }

    @Override
    public boolean delete(ModelId modelId) {
        return false;
    }

    @Override
    public boolean delete(RoutineId routineId) {
        return false;
    }

    @Override
    public boolean delete(JobId jobId) {
        return false;
    }

    @Override
    public Dataset update(DatasetInfo datasetInfo, DatasetOption... options) {
        return null;
    }

    @Override
    public Table update(TableInfo tableInfo, TableOption... options) {
        return null;
    }

    @Override
    public Model update(ModelInfo modelInfo, ModelOption... options) {
        return null;
    }

    @Override
    public Routine update(RoutineInfo routineInfo, RoutineOption... options) {
        return null;
    }

    @Override
    public Table getTable(String datasetId, String tableId, TableOption... options) {
        return null;
    }

    @Override
    public Table getTable(TableId tableId, TableOption... options) {
        return null;
    }

    @Override
    public Model getModel(String datasetId, String modelId, ModelOption... options) {
        return null;
    }

    @Override
    public Model getModel(ModelId tableId, ModelOption... options) {
        return null;
    }

    @Override
    public Routine getRoutine(String datasetId, String routineId, RoutineOption... options) {
        return null;
    }

    @Override
    public Routine getRoutine(RoutineId routineId, RoutineOption... options) {
        return null;
    }

    @Override
    public Page<Routine> listRoutines(String datasetId, RoutineListOption... options) {
        return null;
    }

    @Override
    public Page<Routine> listRoutines(DatasetId datasetId, RoutineListOption... options) {
        return null;
    }

    @Override
    public Page<Table> listTables(String datasetId, TableListOption... options) {
        return null;
    }

    @Override
    public Page<Table> listTables(DatasetId datasetId, TableListOption... options) {
        return null;
    }

    @Override
    public Page<Model> listModels(String datasetId, ModelListOption... options) {
        return null;
    }

    @Override
    public Page<Model> listModels(DatasetId datasetId, ModelListOption... options) {
        return null;
    }

    @Override
    public List<String> listPartitions(TableId tableId) {
        return null;
    }

    @Override
    public InsertAllResponse insertAll(InsertAllRequest request) {
        return null;
    }

    @Override
    public TableResult listTableData(String datasetId, String tableId, TableDataListOption... options) {
        return null;
    }

    @Override
    public TableResult listTableData(TableId tableId, TableDataListOption... options) {
        return null;
    }

    @Override
    public TableResult listTableData(String datasetId, String tableId, Schema schema, TableDataListOption... options) {
        return null;
    }

    @Override
    public TableResult listTableData(TableId tableId, Schema schema, TableDataListOption... options) {
        return null;
    }

    @Override
    public Job getJob(String jobId, JobOption... options) {
        return null;
    }

    @Override
    public Job getJob(JobId jobId, JobOption... options) {
        return null;
    }

    @Override
    public Page<Job> listJobs(JobListOption... options) {
        return null;
    }

    @Override
    public boolean cancel(String jobId) {
        return false;
    }

    @Override
    public boolean cancel(JobId jobId) {
        return false;
    }

    @Override
    public TableResult query(QueryJobConfiguration configuration, JobOption... options)
            throws InterruptedException, JobException {
        return null;
    }

    @Override
    public TableResult query(QueryJobConfiguration configuration, JobId jobId, JobOption... options)
            throws InterruptedException, JobException {
        return null;
    }

    @Override
    public QueryResponse getQueryResults(JobId jobId, QueryResultsOption... options) {
        return null;
    }

    @Override
    public TableDataWriteChannel writer(WriteChannelConfiguration writeChannelConfiguration) {
        return null;
    }

    @Override
    public TableDataWriteChannel writer(JobId jobId, WriteChannelConfiguration writeChannelConfiguration) {
        return null;
    }

    @Override
    public Policy getIamPolicy(TableId tableId, IAMOption... options) {
        return null;
    }

    @Override
    public Policy setIamPolicy(TableId tableId, Policy policy, IAMOption... options) {
        return null;
    }

    @Override
    public List<String> testIamPermissions(TableId table, List<String> permissions, IAMOption... options) {
        return null;
    }

    @Override
    public BigQueryOptions getOptions() {
        return null;
    }
}
