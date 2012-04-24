package gelf4j.log4j;

import gelf4j.GelfTargetConfig;
import gelf4j.TestGelfConnection;
import java.lang.reflect.Field;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.apache.log4j.Category;
import org.apache.log4j.MDC;
import org.apache.log4j.NDC;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

/**
 *
 * @author Anton Yakimov
 * @author Jochen Schalanda
 */
public class GelfAppenderTest {

    private static final String CLASS_NAME = GelfAppenderTest.class.getCanonicalName();
    private TestGelfConnection gelfSender;
    private GelfAppender gelfAppender;

    @Before
    public void setUp() throws Exception {
        gelfSender = new TestGelfConnection(new GelfTargetConfig() );

      gelfAppender = new GelfAppender();
      final Field field = gelfAppender.getClass().getDeclaredField( "_connection" );
      field.setAccessible( true );
      field.set( gelfAppender, gelfSender );
    }

    @Test
    public void ensureHostnameForMessage() {

        LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(GelfAppenderTest.class), 123L, Priority.INFO, "Das Auto",
                                              new RuntimeException("LOL"));
        gelfAppender.append(event);

        assertThat("Message hostname", gelfSender.getLastMessage().getHostname(), notNullValue());

        gelfAppender.setOriginHost("example.com");
        gelfAppender.append(event);
        assertThat(gelfSender.getLastMessage().getHostname(), is("example.com"));
    }

    @Test
    public void handleNullInAppend() {

        LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(this.getClass()), 123L, Priority.INFO, null, new RuntimeException("LOL"));
        gelfAppender.append(event);

        assertThat("Message short message", gelfSender.getLastMessage().getShortMessage(), notNullValue());
        assertThat("Message full message", gelfSender.getLastMessage().getFullMessage(), notNullValue());
    }

    @Test
    public void handleMDC() {

        LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(this.getClass()), 123L, Priority.INFO, "", new RuntimeException("LOL"));
        MDC.put("foo", "bar");

        gelfAppender.append(event);

      assertEquals("bar", gelfSender.getLastMessage().getAdditionalFields().get( "foo" ));
      assertNull( gelfSender.getLastMessage().getAdditionalFields().get( "non-existent" ));
    }

    @Test
    public void handleNDC() {

        LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(this.getClass()), 123L, Priority.INFO, "", new RuntimeException("LOL"));
        NDC.push("Foobar");

        gelfAppender.append(event);

      assertEquals("Foobar", gelfSender.getLastMessage().getAdditionalFields().get( "loggerNdc" ));
    }

    @Test
    public void disableExtendedInformation() {

        LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(this.getClass()), 123L, Priority.INFO, "", new RuntimeException("LOL"));

        MDC.put("foo", "bar");
        NDC.push("Foobar");

        gelfAppender.append(event);

      assertNull( gelfSender.getLastMessage().getAdditionalFields().get( "loggerNdc" ));
      assertNull( gelfSender.getLastMessage().getAdditionalFields().get( "foo" ));
    }

    @Test
    public void checkExtendedInformation() throws UnknownHostException, SocketException {

        LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(GelfAppenderTest.class), 123L, Priority.INFO, "Das Auto", new RuntimeException("LOL"));

        gelfAppender.append(event);

      assertEquals( gelfSender.getLastMessage().getAdditionalFields().get( "logger" ), CLASS_NAME);
    }
}
