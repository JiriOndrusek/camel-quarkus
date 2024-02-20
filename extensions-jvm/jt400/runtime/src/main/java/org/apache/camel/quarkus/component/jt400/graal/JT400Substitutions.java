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

import java.awt.*;
import java.io.IOException;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
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

    //    @Substitute
    //    private void promptSignon() throws AS400SecurityException, IOException {
    //        //do nothing GUI is not available
    //    }

    //todo workaround for stacktrace
    //    at java.awt.Dialog.show(Dialog.java:1047)
    //    at com.ibm.as400.access.ChangePasswordDialog.prompt(ChangePasswordDialog.java:188)
    //    at com.ibm.as400.access.ToolboxSignonHandler.handlePasswordChange(ToolboxSignonHandler.java:431)
    //    at com.ibm.as400.access.ToolboxSignonHandler.passwordAboutToExpire(ToolboxSignonHandler.java:91)
    //    at com.ibm.as400.access.AS400.promptSignon(AS400.java:3386)
    //    at com.ibm.as400.access.AS400.signon(AS400.java:4735)
    @Substitute
    public boolean isInPasswordExpirationWarningDays() throws AS400SecurityException, IOException {
        //skip verification, because it cen end with GUi dialog
        return false;
    }
}

//even if gui is turned off, the presence of code in Dialogs, which references awt object, causes the java.lang.Thread
//initialization error

@TargetClass(className = "com.ibm.as400.access.PasswordDialog")
final class SubstitutePasswordDialog {

    @Substitute
    SubstitutePasswordDialog(Frame parent, String titleText, boolean showCheckbox) {
        //do nothing, gui is turned off
    }

    @Substitute
    boolean prompt() {
        //behave like the dialog was cancelled
        return false;
    }
}

@TargetClass(className = "com.ibm.as400.access.ChangePasswordDialog")
final class SubstituteChangePasswordDialog {

    @Substitute
    SubstituteChangePasswordDialog(Frame parent, String titleText) {
        //do nothing, gui is turned off
    }

    @Substitute
    boolean prompt(String systemName, String userId) {
        //no change
        return false;
    }
}

@TargetClass(className = "com.ibm.as400.access.MessageDialog")
final class SubstituteMessageDialog {

    @Substitute
    SubstituteMessageDialog(Frame parent, String messageText, String titleText, boolean allowChoice) {
        //do nothing, gui is turned off
    }

    @Substitute
    boolean display() {
        //behave like 'No' was pressed
        return false;
    }
}

@TargetClass(value = Frame.class)
final class SubstituteFrame {
    @Substitute
    public SubstituteFrame(String title) throws HeadlessException {
        //do nothing, gui is turned off
    }
}

//@TargetClass(value = Container.class)
//final class SubstituteContainer {
//    @Substitute
//    public Component add(Component comp) {
//        //do nothing, gui is disabled
//        return comp;
//    }
//}
//
//@TargetClass(value = Window.class)
//final class SubstituteWindow {
//    @Substitute
//    private void init(GraphicsConfiguration gc) {
//        //do nothing, gui is disabled
//    }
//}

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
