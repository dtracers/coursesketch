package connection;

import coursesketch.database.DatabaseClient;
import coursesketch.server.base.GeneralConnectionRunner;
import coursesketch.server.base.ServerWebSocketInitializer;
import coursesketch.server.interfaces.ServerInfo;
import database.DatabaseStringConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts the login server.
 *
 * @author gigemjt
 */
public final class LoginRunner extends GeneralConnectionRunner {

    /**
     * Declaration and Definition of Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(LoginRunner.class);

    /**
     * The port on which the login server lies.
     */
    public static final int LOGIN_PORT = 8886;

    /**
     * The actual main method that starts the login server.
     *
     * @param args
     *            Arguments passed from the command line.
     */
    public static void main(final String... args) {
        final LoginRunner run = new LoginRunner(args);
        run.start();
    }

    /**
     * @param args
     *            Arguments passed from the command line.
     */
    public LoginRunner(final String... args) {
        super(args);
        super.setPort(LOGIN_PORT);
    }

    /**
     * Makes the login database run locally.
     */
    @Override
    public void executeLocalEnvironment() {
        LOG.info("Setting the login database to connect locally");
        new DatabaseClient(false, null); // makes the database point locally
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected void loadConfigurations() {
        super.setDatabaseName(DatabaseStringConstants.LOGIN_DATABASE);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link LoginServlet}.
     */
    @Override
    public ServerWebSocketInitializer createSocketInitializer(final ServerInfo serverInfo) {
        return new LoginServlet(serverInfo);
    }
}
