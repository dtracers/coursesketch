package utilities;

/**
 * Created by gigemjt on 4/5/15.
 */
public class SecureServerException extends Exception {
    /**
     * A simple constructor.
     *
     * @param string
     *            The message for the exception.
     */
    public SecureServerException(final String string) {
        super(string);
    }

    /**
     * Passes up an exception as the cause of the exception.
     * @param string A message that gives details of the error
     * @param cause The cause of the exception.
     */
    public SecureServerException(final String string, final Exception cause) {
        super(string, cause);
    }

}
