package handlers;

import coursesketch.database.auth.AuthenticationException;
import coursesketch.database.auth.AuthenticationResponder;
import coursesketch.database.auth.Authenticator;
import coursesketch.database.submission.SubmissionManagerInterface;
import database.DatabaseAccessException;
import database.SubmissionDatabaseClient;
import protobuf.srl.school.School;
import protobuf.srl.services.authentication.Authentication;
import protobuf.srl.submission.Submission;
import utilities.TimeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dtracers on 12/17/2015.
 */
public final class SubmissionManager implements SubmissionManagerInterface {

    /**
     * The interface for the mongo database.
     */
    private final SubmissionDatabaseClient submissionDatabaseClient;

    /**
     * A constructor that takes in a {@link SubmissionDatabaseClient}.
     * @param submissionDatabaseClient The interface for the mongo database.
     */
    public SubmissionManager(final SubmissionDatabaseClient submissionDatabaseClient) {
        this.submissionDatabaseClient = submissionDatabaseClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override public List<Submission.SrlExperiment> getSubmission(final String authId, final Authenticator authenticator,
            final String problemId, final String... submissionIds) throws DatabaseAccessException, AuthenticationException {
        final Authentication.AuthType.Builder authType = Authentication.AuthType.newBuilder();
        authType.setCheckingAdmin(true);
        final AuthenticationResponder authenticationResponder = authenticator
                .checkAuthentication(School.ItemType.COURSE_PROBLEM, problemId, authId, TimeManager.getSystemTime(),
                        authType.build());
        if (!authenticationResponder.hasStudentPermission()) {
            throw new AuthenticationException("User does not have permission to for this submission:", AuthenticationException.INVALID_PERMISSION);
        }

        final List<Submission.SrlExperiment> experiments = new ArrayList<>();
        for (String submission: submissionIds) {
            experiments.add(submissionDatabaseClient.getExperiment(submission, problemId, authenticationResponder));
        }
        return experiments;
    }

    /**
     * {@inheritDoc}
     */
    @Override public String insertExperiment(final String authId, final Authenticator authenticator, final Submission.SrlExperiment submission,
            final long submissionTime)
            throws AuthenticationException, DatabaseAccessException {
        final String problemId = submission.getProblemId();
        final Authentication.AuthType.Builder authType = Authentication.AuthType.newBuilder();
        authType.setCheckingAdmin(true);
        final AuthenticationResponder authenticationResponder = authenticator
                .checkAuthentication(School.ItemType.COURSE_PROBLEM, problemId, authId, TimeManager.getSystemTime(),
                        authType.build());
        if (!authenticationResponder.hasStudentPermission()) {
            throw new AuthenticationException("User does not have permission to for this submission", AuthenticationException.INVALID_PERMISSION);
        }

        return submissionDatabaseClient.saveExperiment(submission, submissionTime);
    }

    /**
     * {@inheritDoc}
     */
    @Override public String insertSolution(final String authId, final Authenticator authenticator, final Submission.SrlSolution submission)
            throws AuthenticationException, DatabaseAccessException {
        final String problemBankId = submission.getProblemBankId();
        final Authentication.AuthType.Builder authType = Authentication.AuthType.newBuilder();
        authType.setCheckingAdmin(true);
        final AuthenticationResponder authenticationResponder = authenticator
                .checkAuthentication(School.ItemType.COURSE_PROBLEM, problemBankId, authId, TimeManager.getSystemTime(),
                        authType.build());
        if (!authenticationResponder.hasModeratorPermission()) {
            throw new AuthenticationException("User does not have permission to for this submission", AuthenticationException.INVALID_PERMISSION);
        }

        return submissionDatabaseClient.saveSolution(submission);
    }
}
