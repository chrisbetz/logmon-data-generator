import com.aphyr.riemann.client.EventDSL;
import com.aphyr.riemann.client.RiemannClient;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;

/**
 * Created by cbetz on 09.12.15.
 */
public class RiemannAppender extends AppenderSkeleton {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5555;
    private static final int DEFAULT_RECONNECTION_DELAY = 1000;

    private String host = DEFAULT_HOST;
    private int port = DEFAULT_PORT;
    private int reconnectionDelay = DEFAULT_RECONNECTION_DELAY;
    private RiemannClient riemann;
    private long lastConnectionAttempt = Long.MIN_VALUE;

    private String localHostname = null;
    private String localServicename = null;

    public String getLocalHostname() {
        return localHostname;
    }

    public void setLocalHostname(String localHostname) {
        this.localHostname = localHostname;
    }

    public String getLocalServicename() {
        return localServicename;
    }

    public void setLocalServicename(String localServicename) {
        this.localServicename = localServicename;
    }

    public static boolean isDebug() {
        return debug;
    }

    public static void setDebug(boolean debug) {
        RiemannAppender.debug = debug;
    }

    private static boolean debug = true;


//////////////////////////////////////////////////////////
// setters and getters

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getReconnectionDelay() {
        return reconnectionDelay;
    }

    public void setReconnectionDelay(int reconnectionDelay) {
        this.reconnectionDelay = reconnectionDelay;
    }
//////////////////////////////////////////////////////////


    @Override
    public String toString() {
        return "RiemannAppender{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }

    @Override
    public void activateOptions() {
        super.activateOptions();
        try {
            riemann = RiemannClient.tcp(getHost(), getPort());
            riemann.connect();
        } catch (IOException e) {
            if (debug) {
                System.err.println(String.format("%s.activateOptions:Problem connecting to riemann: %s", this, e));
                e.printStackTrace();
            }
        }

    }


    private void ensureConnection() {
        long now = System.currentTimeMillis();
        boolean unconnected = false;
        try {
            unconnected = !riemann.isConnected();
        } catch (Exception e) {
            if (debug) {
                e.printStackTrace();
            }
        }
        if (unconnected && lastConnectionAttempt + reconnectionDelay < now) {
            try {
                riemann.reconnect();
            } catch (IOException e) {
                if (debug) {
                    e.printStackTrace();
                }
            } finally {
                lastConnectionAttempt = now;
            }
        }
    }


    @Override
    protected void append(LoggingEvent loggingEvent) {
        if (riemann != null) {
            ensureConnection();
            if (riemann.isConnected()) {
                EventDSL rEvent = riemann.event()
                        .host(localHostname)
                        .service((localServicename != null ? localServicename + " " : "") + loggingEvent.getLoggerName())
                        .state(loggingEvent.getLevel().toString())
                        .attribute("message", loggingEvent.getRenderedMessage())
                        .attribute("thread", loggingEvent.getThreadName());

                if (loggingEvent.locationInformationExists()) {
                    rEvent.attribute("location-class", loggingEvent.getLocationInformation().getClassName());
                    rEvent.attribute("location-method", loggingEvent.getLocationInformation().getMethodName());
                    rEvent.attribute("location-file", loggingEvent.getLocationInformation().getFileName());
                    rEvent.attribute("location-line", loggingEvent.getLocationInformation().getLineNumber());
                }

                if (loggingEvent.getThrowableInformation() != null) {
                    rEvent.attribute("stacktrace", getStacktraceString(loggingEvent.getThrowableStrRep()));
                }


                try {
                    rEvent.send();
                } catch (Exception e) {
                    if (debug) {
                        System.err.println(e);
                    }
                    try {
                        ensureConnection();
                        rEvent.send();
                    } catch (Exception nestedException) {
                        if (debug) {
                            System.err.println(nestedException);
                        }
                    }
                }
            }
        } else if (debug) {
            System.err.println("Trying to write to an undefined Riemann instance.");
        }
    }

    private String getStacktraceString(String[] throwableStrRep) {
        StringBuilder sb = new StringBuilder();
        for (String elt : throwableStrRep) {
            sb.append(elt);
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public void close() {
        if (riemann != null) {
            try {
                riemann.close();
            } catch (Exception e) {
                if (debug) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
