package org.apache.camel.quarkus.component.mail;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import javax.mail.internet.MimeMessage;


public class ExampleReceiveTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

    @Test
    public void testReceive() {
        GreenMailUser user = greenMail.setUser("to@localhost", "login-id", "password");
        user.deliver(createMimeMessage()); // You can either create a more complex message...
        GreenMailUtil.sendTextEmailTest("to@localhost", "from@localhost",
                "subject", "body"); // ...or use the default messages

        Assertions.assertTrue(greenMail.getReceivedMessages().length == 2); // // --- Place your POP3 or IMAP retrieve code here
    }

    private MimeMessage createMimeMessage() {
        return GreenMailUtil.createTextEmail("to@localhost", "from@localhost", "subject", "body",
            greenMail.getImap().getServerSetup());
    }
}
