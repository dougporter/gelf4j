package org.logbackgelf;

import ch.qos.logback.core.AppenderBase;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Responsible for Formatting a log event and sending it to a Graylog2 Server. Note that you can't swap in a different
 * Layout since the GELF format is static.
 */
public class GelfAppender<E> extends AppenderBase<E> {

    private final Transport transport;

    // Defaults for variables up here
    private String facility = "GELF";
    private String graylog2ServerHost = "localhost";
    private int graylog2ServerPort = 12201;
    private boolean useLoggerName = false;
    private Map<String, String> additionalFields = new HashMap<String, String>();
    private int shortMessageLength = 255;

    public GelfAppender() {

        transport = new Transport();
    }

    /**
     * The main append method. Takes the event that is being logged, formats if for GELF and then sends it over the wire
     * to the log server
     *
     * @param eventObject The event that we are logging
     */
    @Override
    protected void append(E eventObject) {

        GelfConverter converter = new GelfConverter(facility, useLoggerName, additionalFields, shortMessageLength);

        try {

            byte[] packet = gzipString(converter.convertToGelf(eventObject));

            transport.sendPacket(packet, graylog2ServerHost, graylog2ServerPort);

        } catch (RuntimeException e) {

            this.addError("Error occurred: ", e);
        }
    }

    /**
     * zips up a string into a GZIP format.
     *
     * @param str The string to zip
     * @return The zipped string
     */
    private byte[] gzipString(String str) {
        GZIPOutputStream zipStream = null;
        try {
            ByteArrayOutputStream targetStream = new ByteArrayOutputStream();
            zipStream = new GZIPOutputStream(targetStream);
            zipStream.write(str.getBytes());
            zipStream.close();
            byte[] zipped = targetStream.toByteArray();
            targetStream.close();
            return zipped;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (zipStream != null) {
                    zipStream.close();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    //////////////////// Property Getter/Setters (so they can be changed in config) /////////////////////////

    /**
     * The name of your service. Appears in facility column in graylog2-web-interface
     */
    public String getFacility() {
        return facility;
    }

    public void setFacility(String facility) {
        this.facility = facility;
    }

    /**
     * The hostname of the graylog2 server to send messages to
     */
    public String getGraylog2ServerHost() {
        return graylog2ServerHost;
    }

    public void setGraylog2ServerHost(String graylog2ServerHost) {
        this.graylog2ServerHost = graylog2ServerHost;
    }

    /**
     * The port of the graylog2 server to send messages to
     */
    public int getGraylog2ServerPort() {
        return graylog2ServerPort;
    }

    public void setGraylog2ServerPort(int graylog2ServerPort) {
        this.graylog2ServerPort = graylog2ServerPort;
    }

    /**
     * If true, an additional field call "_loggerName" will be added to each gelf message. Its contents will be the fully
     * qualified name of the logger. e.g: com.company.Thingo.
     */
    public boolean isUseLoggerName() {
        return useLoggerName;
    }

    public void setUseLoggerName(boolean useLoggerName) {
        this.useLoggerName = useLoggerName;
    }

    /**
     * additional fields to add to the gelf message. Here's how these work:
     * <br/>
     * Let's take an example. I want to log the client's ip address of every request that comes into my web server. To do this,
     * I add the ipaddress to the slf4j MDC on each request as follows:
     * <code>
     * ...
     * MDC.put("ipAddress", "44.556.345.657");
     * ...
     * </code>
     * Now, to include the ip address in the gelf message, i just add the following to my logback.groovy:
     * <code>
     * appender("GELF", GelfAppender) {
     *   ...
     *   additionalFields = [identity:"_identity"]
     *   ...
     * }
     * </code>
     * in the additionalFields map, the key is the name of the MDC to look up. the value is the name that should be given to
     * the key in the additional field in the gelf message.
     */
    public Map<String, String> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(Map<String, String> additionalFields) {
        this.additionalFields = additionalFields;
    }

    /**
     * The length of the message to truncate to
     */
    public int getShortMessageLength() {
        return shortMessageLength;
    }

    public void setShortMessageLength(int shortMessageLength) {
        this.shortMessageLength = shortMessageLength;
    }
}
