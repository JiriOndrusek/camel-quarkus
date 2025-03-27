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
package org.apache.camel.quarkus.component.cics.graal;

import com.oracle.svm.core.annotate.Substitute;

final class CicsSubstitutions {
}

//@TargetClass(value = CICSTrace.class)
final class SubstituteCICSTrace {

    @Substitute
    public void ex(Object var1, Throwable var2) {
        System.out.println("Error");
    }

    @Substitute
    private static void logEvent(String var0, String var1, String var2, String var3) {
        System.out.println("Error");
    }

    @Substitute
    private static void logEntry(String var0, String var1, String var2, String var3) {
        System.out.println("Error");
    }

    @Substitute
    private static void logExit(String var0, String var1, String var2, String var3) {
        System.out.println("Error");

    }
}
/*
@TargetClass(value = LocalWorker.class)
final class SubstituteCicsLocalWorker {
    @Substitute
    public void execute() throws IOException {
        Log.error("LocalWorker execute is not enabled in the native!");
    }
}

@TargetClass(value = LocalJavaGateway.class)
final class SubstituteLocalJavaGateway {

    @Substitute
    int flow(GatewayRequest var1) throws IOException {
        Log.error("LocalWorker 'flow' is not enabled in the native!");
        return -1;
    }

    @Substitute
    void close() throws IOException {
        Log.error("LocalWorker 'close' is not enabled in the native!");
    }

    //    @Alias
    //    boolean bOpen;
    //    @Alias
    //    private Date expiry;
    //    @Alias
    //    Properties protocolProperties;
    //    @Alias
    //    int socketConnectTimeout = 0;
    //    @Alias
    //    private static Hashtable<String, Class<?>> requestObjs;
    //    @Alias
    //    private static Hashtable<Class<?>, GatewayRequest> hasServerRequests
    //
    //    int flow(GatewayRequest var1) throws IOException {
    //        T.in(this, "flow");
    //        synchronized(this) {
    //            if (!this.bOpen) {
    //                IOException var31 = new IOException(ClientMessages.getMessage((ResourceWrapper)null, 66));
    //                T.ex(this, var31);
    //                throw var31;
    //            }
    //        }
    //
    //        if (CTGType.getType().equals(CTGType.OpenBeta)) {
    //            Date var2 = new Date();
    //            if (var2.after(this.expiry)) {
    //                throw new IOException("CICS Transaction Gateway open beta has expired");
    //            }
    //        }
    //
    //        if (this.protocolProperties == null) {
    //            this.protocolProperties = new Properties();
    //        }
    //
    //        this.protocolProperties.put("SOCKETCONNECTTIMEOUT", this.socketConnectTimeout);
    //        String var29 = var1.getRequestType();
    //        if (var29.indexOf(46) == -1) {
    //            var29 = "com.ibm.ctg.server.Server" + var29 + "Request";
    //        }
    //
    //        T.ln(this, "Need object object of type = {0}", var29);
    //        Object var3 = null;
    //        Object var4 = null;
    //        Class var5 = (Class)requestObjs.get(var29);
    //        if (var5 == null) {
    //            try {
    //                T.ln(this, "First time we have seen a request of type " + var29);
    //                var5 = Class.forName(var29);
    //            } catch (ClassNotFoundException var23) {
    //                T.ex(this, var23);
    //                var1.setRc(61442);
    //                throw new IOException(ClientMessages.getMessage((ResourceWrapper)null, 71, var23));
    //            }
    //        }
    //
    //        GatewayRequest var30;
    //        try {
    //            var30 = (GatewayRequest)var5.newInstance();
    //            var var32 = (ServerGatewayRequest)var30;
    //            var32.setLocalMode(true);
    //            GatewayRequest var6 = (GatewayRequest)var5.newInstance();
    //            synchronized(hasServerRequests) {
    //                if (hasServerRequests.put(var5, var6) == null) {
    //                    T.ln(this, "First time this type has been seen");
    //
    //                    try {
    //                        var6.initialize();
    //                    } catch (Exception var22) {
    //                        hasServerRequests.remove(var5);
    //                        T.ln(this, "Removed entry from seen-type hashtable");
    //                        throw var22;
    //                    }
    //                }
    //            }
    //        } catch (Exception var27) {
    //            T.ex(this, var27);
    //            var1.setRc(61442);
    //            throw new IOException(ClientMessages.getMessage((ResourceWrapper)null, 71, var27));
    //        }
    //
    //        var30.setContentsFromPartner(var1);
    //        var1.localFlowOccurred();
    //        var30.setConnectionIndex(1);
    //        var30.setServerProtocolProperties(this.protocolProperties);
    //        String var33 = null;
    //        String var7 = null;
    //        if (this.protocolProperties != null) {
    //            var33 = this.protocolProperties.getProperty("CTG_APPLID");
    //            var7 = this.protocolProperties.getProperty("CTG_APPLIDQUALIFIER");
    //        }
    //
    //        var30.fireRequestExits(RequestEvent.RequestEntry, this.requestExitMonitor, var33, var7, (String)null, (String)null);
    //        GatewayIntercept.InterceptAction var8 = this.jgOwner.interceptFlow(var1);
    //        if (this.jgOwner.isInterceptPluginActive()) {
    //            var30.setContentsFromPartner(var1);
    //        }
    //
    //        label175: {
    //            byte var11;
    //            try {
    //                if (var8 != GatewayIntercept.InterceptAction.Continue) {
    //                    break label175;
    //                }
    //
    //                synchronized(this.activeRequests) {
    //                    this.activeRequests.put(var32, var32);
    //                }
    //
    //                LocalWorker var9 = new LocalWorker(this, var1, var30);
    //                if (!var30.confirmationRequired()) {
    //                    var9.execute();
    //                    break label175;
    //                }
    //
    //                T.ln(this, "This is an async request so do the work in another thread");
    //                Thread var10 = new Thread(var9);
    //                var10.setDaemon(true);
    //                var10.start();
    //                T.out(this, "flow", 0);
    //                var11 = 0;
    //            } catch (Exception var24) {
    //                T.ex(this, var24);
    //                throw new IOException(ClientMessages.getMessage((ResourceWrapper)null, 71, var24));
    //            } finally {
    //                var30.fireRequestExits(RequestEvent.ResponseExit, this.requestExitMonitor, var33, var7, (String)null, (String)null);
    //            }
    //
    //            return var11;
    //        }
    //
    //        int var34 = var1.getRc();
    //        T.out(this, "flow", var34);
    //        return var34;
    //    }
    //
    //    @TargetClass(value = StatFilter.class)
    //    final class SubstituteStatFilter {
    //
    //        @Substitute
    //        public SubstituteStatFilter(StatFilter.StatFilterType var1, String var2) {
    //            Log.error("LocalWorker 'close' is not enabled in the native!");
    //        }

    //        public void  StatFilter(StatFilter.StatFilterType var1, String var2) {
    //            this.activeFilterType = StatFilter.StatFilterType.NULLFILTER;
    //            this.statTypeMask = null;
    //            this.statNameMask = null;
    //            T.in(this, "filterType,mask", var1, var2);
    //            switch (var1) {
    //                case STATTYPE:
    //                    this.activeFilterType = StatFilter.StatFilterType.STATTYPE;
    //                    Object var3 = null;
    //                    this.statTypeMask = new Vector();
    //                    char[] var4 = var2.toCharArray();
    //                    int var5 = var4.length;
    //
    //                    for(int var6 = 0; var6 < var5; ++var6) {
    //                        Character var7 = var4[var6];
    //                        Stat.StatType var8 = StatType.getStatType(var7);
    //                        if (var8 != null) {
    //                            this.statTypeMask.add(var8);
    //                        } else if (var7 != ':') {
    //                            CTGCtrlUtil.PrintMsg(CTGCtrlUtil.getMsg((ResourceBundle)null, "STATS_BAD_STATTYPE", new Object[]{var7}));
    //                        }
    //                    }
    //
    //                    if (this.statTypeMask.isEmpty()) {
    //                        T.ln(this, "filter contined no valid stats types. Setting NULLFILTER");
    //                        this.activeFilterType = StatFilter.StatFilterType.NULLFILTER;
    //                        this.statTypeMask = null;
    //                    } else {
    //                        T.ln(this, "statTypeMask={0}", this.statTypeMask);
    //                    }
    //                    break;
    //                case STATNAME:
    //                    this.activeFilterType = StatFilter.StatFilterType.STATNAME;
    //                    this.statNameMask = var2;
    //                    T.ln(this, "statNameMask={0}", this.statNameMask);
    //                    break;
    //                case NULLFILTER:
    //                    this.activeFilterType = StatFilter.StatFilterType.NULLFILTER;
    //                    break;
    //                default:
    //                    this.activeFilterType = StatFilter.StatFilterType.NULLFILTER;
    //            }
    //
    //            T.out(this, "activeFilterType", this.activeFilterType);
    //    }\\
}
*/
