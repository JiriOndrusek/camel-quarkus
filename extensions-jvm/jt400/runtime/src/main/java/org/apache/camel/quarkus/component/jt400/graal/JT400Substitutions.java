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
package org.apache.camel.quarkus.component.jt400.graal;

import java.io.IOException;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.PasswordDialog;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

final class JT400Substitutions {
}

@TargetClass(value = AS400.class)
final class SubstituteAS400 {
    @Substitute
    public boolean isGuiAvailable() {
        return false;
    }

    @Substitute
    private void promptSignon() throws AS400SecurityException, IOException {
        //do nothing GUI is not available
    }
}

//todo workarounf for stack trace:
//    at java.awt.Dialog.setBackground(Dialog.java:1337)
//    at com.ibm.as400.access.PasswordDialog.<init>(PasswordDialog.java:78)
//    at com.ibm.as400.access.ToolboxSignonHandler.setupPasswordDialog(ToolboxSignonHandler.java:573)
//    at com.ibm.as400.access.ToolboxSignonHandler.handleSignon(ToolboxSignonHandler.java:496)
//    at com.ibm.as400.access.ToolboxSignonHandler.connectionInitiated(ToolboxSignonHandler.java:50)
//    at com.ibm.as400.access.AS400.promptSignon(AS400.java:3360)
//    at com.ibm.as400.access.AS400.signon(AS400.java:4736)
@TargetClass(value = PasswordDialog.class)
final class SubstitutePasswordDialog {
    @Substitute
    boolean prompt() {
        //behave like the dialog was cancelled
        return false;
    }
}

// todo workaro8und for stacktrace
//    at java.awt.Window.pack(Window.java:829)
//    at com.ibm.as400.access.MessageDialog.<init>(MessageDialog.java:107)
//    at com.ibm.as400.access.ToolboxSignonHandler.displayMessage(ToolboxSignonHandler.java:336)
//    at com.ibm.as400.access.ToolboxSignonHandler.userIdDisabled(ToolboxSignonHandler.java:248)
//    at com.ibm.as400.access.AS400.promptSignon(AS400.java:3425)
//    at com.ibm.as400.access.AS400.signon(AS400.java:4736)
//    at com.ibm.as400.access.AS400.getCcsid(AS400.java:1925)
@TargetClass(className = "com.ibm.as400.access.MessageDialog")
final class SubstituteMessageDialog {
    @Substitute
    boolean display() {
        //behave like 'No' was pressed
        return false;
    }
}

//todo workaround for stacktrace
//    at java.awt.Dialog.<init>(Dialog.java:674)
//    at com.ibm.as400.access.PasswordDialog.<init>(PasswordDialog.java:64)
//    at com.ibm.as400.access.ToolboxSignonHandler.setupPasswordDialog(ToolboxSignonHandler.java:573)
//    at com.ibm.as400.access.ToolboxSignonHandler.handleSignon(ToolboxSignonHandler.java:496)
//    at com.ibm.as400.access.ToolboxSignonHandler.userIdDisabled(ToolboxSignonHandler.java:250)
//    at com.ibm.as400.access.AS400.promptSignon(AS400.java:3425)
//    at com.ibm.as400.access.AS400.signon(AS400.java:4736)
//    at com.ibm.as400.access.AS400.getCcsid(AS400.java:1925)

//@TargetClass(className = "com.ibm.as400.access.ToolboxSignonHandler")
//final class SubstituteToolboxSignonHandler {
//    @Substitute
//    public boolean passwordAboutToExpire(SignonEvent event, int daysUntilExpiration) {
//        return true; // do nothing, proceed with sign-on
//    }
//
//    @Substitute
//    public boolean connectionInitiated(SignonEvent event, boolean forceUpdate) {
//        return false;
//    }
//}
