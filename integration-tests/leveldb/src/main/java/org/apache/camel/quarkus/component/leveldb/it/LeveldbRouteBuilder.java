/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.camel.quarkus.component.leveldb.it;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.leveldb.LevelDBAggregationRepository;

public class LeveldbRouteBuilder extends RouteBuilder {
    public static final String DIRECT_START = "direct:start";
    public static final String DIRECT_START_WITH_FAILURE = "direct:startWithFailure";
    public static final String MOCK_AGGREGATED = "mock:aggregated";
    public static final String MOCK_RESULT = "mock:result";

    private static AtomicInteger counter = new AtomicInteger(0);

    @Override
    public void configure() throws Exception {
        // create the leveldb repo
        LevelDBAggregationRepository repo = new LevelDBAggregationRepository("repo", "target/data/leveldb.dat");

        from(DIRECT_START)
                .aggregate(header("id"), new MyAggregationStrategy())
                .log("aggregated exchange id ${exchangeId} with ${body}")
                // use our created leveldb repo as aggregation repository
                .completionSize(7).aggregationRepository(repo)
                .to(MOCK_RESULT);

        LevelDBAggregationRepository repoWithFailure = new LevelDBAggregationRepository("repoWithFailure",
                "target/data/leveldbWithFailure.dat");

        // enable recovery
        repoWithFailure.setUseRecovery(true);
        // check faster
        repoWithFailure.setRecoveryInterval(500, TimeUnit.MILLISECONDS);

        from(DIRECT_START_WITH_FAILURE)
                .log("sending exchange id ${exchangeId} with ${body}")
                .aggregate(header("id"), new MyAggregationStrategy())
                .completionSize(7).aggregationRepository(repoWithFailure)
                .to(MOCK_AGGREGATED)
                .log("aggregated exchange id ${exchangeId} with ${body}")
                .delay(1000)
                // simulate errors the first two times
                .process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        int count = counter.incrementAndGet();
                        if (count <= 2) {
                            System.out.println("=============== failure ==============");
                            throw new IllegalArgumentException("Damn");
                        }
                    }
                })
                .log("result exchange id ${exchangeId} with ${body}")
                .to(MOCK_RESULT)
                .end();
    }

    public static class MyAggregationStrategy implements AggregationStrategy {

        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            if (oldExchange == null) {
                return newExchange;
            }
            String body1 = oldExchange.getIn().getBody(String.class);
            String body2 = newExchange.getIn().getBody(String.class);

            oldExchange.getIn().setBody(body1 + body2);
            return oldExchange;
        }
    }
}
