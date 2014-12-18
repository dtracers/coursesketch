package serverfront;

import coursesketch.server.base.GeneralConnectionRunner;
import coursesketch.server.base.ServerWebSocketInitializer;

/**
 * A subclass of the runner and sets up some special information for running the
 * environment.
 */
public class ProxyRunner extends GeneralConnectionRunner {

    /** 30 minutes * 60 seconds * 1000 milliseconds. */
    private static final long TIMEOUT_TIME = 30 * 60 * 1000;

    /** port of the proxy server. */
    private static final int PROXY_PORT = 8888;

    /**
     * @param args
     *            arguments from the command line.
     */
    public static void main(final String[] args) {
        final ProxyRunner run = new ProxyRunner(args);
        run.start();
    }

    /**
     * sets some SSL information. FUTURE: this should be read from a file
     * instead of listed in code.
     */
    @Override
    public final void executeRemoveEnvironment() {
        setCertificatePath("Challeng3");
        setKeystorePath("srl01_tamu_edu.jks");
    }

    /**
     * Creates a new proxy runner.
     *
     * @param args
     *            arguments from the command line.
     */
    public ProxyRunner(final String[] args) {
        super(args);
        super.setPort(PROXY_PORT);
        super.setTimeoutTime(TIMEOUT_TIME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ServerWebSocketInitializer createSocketInitializer(final long time, final boolean secure, final boolean local) {
        return new ProxyServlet(time, secure, local);
    }
}