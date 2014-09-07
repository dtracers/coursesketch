package database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import protobuf.srl.commands.Commands.CommandType;
import protobuf.srl.commands.Commands.SrlCommand;
import protobuf.srl.commands.Commands.SrlUpdate;
import protobuf.srl.commands.Commands.SrlUpdateList;
import protobuf.srl.request.Message.Request;
import protobuf.srl.submission.Submission.SrlExperiment;
import protobuf.srl.submission.Submission.SrlSolution;
import protobuf.srl.submission.Submission.SrlSubmission;

import com.google.protobuf.InvalidProtocolBufferException;

public class UpdateHandler {
	HashMap<String, ListInstance> sessionToInstance = new HashMap<String, ListInstance>();
	public boolean addRequest(Request req) throws Exception {
		SrlUpdateList updates = null;
		System.out.println("Adding request!");
		if (!sessionToInstance.containsKey(req.getSessionInfo())) {
			System.out.println("NEW INSTANCE!");
			SubmissionInstance instance = new SubmissionInstance();
			sessionToInstance.put(req.getSessionInfo(), instance);
			try {
				if (req.getResponseText().equals("student")) {
					SrlExperiment experiment = SrlExperiment.parseFrom(req.getOtherData());
					instance.setExperiment(experiment);
					instance.setSubmissionTime(req.getMessageTime());
					instance.setUserId(req.getServersideId());
					return false;
				}
				SrlSolution solution = SrlSolution.parseFrom(req.getOtherData());
				instance.setSolution(solution);
				return false;
				// if this is the first submission it will contain stuff regarding that submission
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		if (updates == null) {
			try {
				updates = SrlUpdateList.parseFrom(req.getOtherData());
			} catch (InvalidProtocolBufferException e) {
				//e.printStackTrace();
				System.out.println("Parsing as a single update");
			}
			if (updates == null) {
				try {
					SrlUpdate update = SrlUpdate.parseFrom(req.getOtherData());
					updates = SrlUpdateList.newBuilder().addList(update).build();
				} catch(InvalidProtocolBufferException e) {
					e.printStackTrace();
				}
			}
		}
		if (updates == null) {
			System.out.println(req);
			//throw new Exception("Mismatched Message Exception");
		}
		return sessionToInstance.get(req.getSessionInfo()).addRequest(updates);
	}

	public void clearSubmission(String sessionKey) {
		sessionToInstance.remove(sessionKey);
	}

	public boolean hasSubmissionId(String sessionInfo) {
		return ((SubmissionInstance) sessionToInstance.get(sessionInfo)).hasId();
	}
	
	public String getSubmissionId(String sessionInfo) {
		return ((SubmissionInstance) sessionToInstance.get(sessionInfo)).getId();
	}

	public boolean isSolution(String sessionInfo) {
		return ((SubmissionInstance) sessionToInstance.get(sessionInfo)).isSolution();
	}
	
	public SrlSolution getSolution(String sessionInfo) throws Exception {
		return ((SubmissionInstance) sessionToInstance.get(sessionInfo)).getSolution();
	}
	
	public SrlExperiment getExperiment(String sessionInfo) throws Exception {
		return ((SubmissionInstance) sessionToInstance.get(sessionInfo)).getExperiment();
	}

	/**
	 * Allows a list of updates to be built slowly over time ensuring the correct order.
	 *
	 * The correct ordered will happen even if they arrive out of order.
	 * @author gigemjt
	 *
	 */
	private class ListInstance {
		SrlUpdateList.Builder list = SrlUpdateList.newBuilder();
		protected SrlUpdateList result;

		private boolean started = false;
		private boolean finished = false;
		private int currentIndex = -1;
		ArrayList<SrlUpdate> buffer = new ArrayList<SrlUpdate>();
		
		public boolean isFinished() {
			return finished;
		}

		/**
		 * There are three different things that can be submitted.
		 * #1 an SrlUpdateList
		 * #2 an SrlUpdate
		 * @param req
		 */
		public boolean addRequest(SrlUpdateList updates) {
			if (finished) {
				return true;
			}
			try {
				List<SrlUpdate> updateList = updates.getListList();
				for (SrlUpdate update: updateList) {
					List<SrlCommand> commandList = update.getCommandsList();
					if (commandList.size() == 0) {
						throw new Exception("EMPTY LIST!");
					}
					if(!started) {
						System.out.println("UPDATE: " + update);
					}
					if (started && commandList.get(commandList.size()-1).getCommandType() == CommandType.CLOSE_SYNC) {
						started = false;
						finished = true;
						result = list.build();
						System.out.println("Finished parsing list!");
						return true;
					}
					if (!started && commandList.get(0).getCommandType() == CommandType.OPEN_SYNC) {
						System.out.println("RECIEVED OPEN SYNC");
						started = true;
					} else if (!started) {
						insertInBuffer(update);
					} else {
						if (update.getCommandNumber() == currentIndex + 1) {
							addToList(update);
							currentIndex +=1;
							searchBuffer();
						}
					}
				}
				return false;
			} catch(Exception e) {
				e.printStackTrace();
			}
			return false;
		}

		/**
		 * Goes through the sorted buffer and checks to see if the next element has the correct number.
		 *
		 */
		private void searchBuffer() {
			SrlUpdate nextUpdate = null;
			while (buffer.size() > 0) {
				nextUpdate = buffer.get(0);
				if (nextUpdate.getCommandNumber() == currentIndex + 1) {
					buffer.remove(0);
					addToList(nextUpdate);
					currentIndex +=1;
				} else {
					break; // quit going we will try again later.
				}
			}
		}

		/**
		 * Inserts the update into the buffer.
		 *
		 * The list will be sorted before this insertion and it will remain sorted after this insertion.
		 */
		private void insertInBuffer(SrlUpdate update) {
			System.out.println("INSRTING INTO BUFFER!");
			// TODO Insert an update into the list so that the list is always sorted.
		}

		private void addToList(SrlUpdate update) {
			list.addList(update);
			// may execute the action while inserting to the list.
			// this will be called in a sorted order.
		}
	}

	private class SubmissionInstance extends ListInstance {
		com.google.protobuf.GeneratedMessage submission = null;
		private boolean isSolution = false;
		private boolean hasId = false;
		private String id = null;
		private String userId = null;
		private long messageTime;
		public boolean isSolution() {
			return isSolution;
		}

		public void setUserId(String serversideId) {
			userId = serversideId;
		}

		public void setSubmissionTime(long messageTime) {
			this.messageTime = messageTime;
		}

		public void setExperiment(SrlExperiment experiment) {
			isSolution = false;
			submission = experiment;
			hasId = experiment.getSubmission().hasId();
			id = experiment.getSubmission().getId();
			if (id == null || id.equals("" ) || id.equals("NO_ID")) {
				System.out.println("ID IS NULL? " + id);
				hasId = false;
				id = null;
			}
		}
		
		public void setSolution(SrlSolution solution) {
			isSolution = true;
			submission = solution;
			hasId = solution.getSubmission().hasId();
			id = solution.getSubmission().getId();
			if (id == null || id.equals("" )) {
				hasId = false;
				id = null;
			}
		}

		public SrlSolution getSolution() throws Exception {
			if (!isSolution) {
				throw new Exception("Incorrect Message Type: cannot retrieve solution");
			}
			if (!this.isFinished()) {
				throw new Exception("cannot retrieve incomplete submission");
			}
			SrlSolution.Builder builder = SrlSolution.newBuilder((SrlSolution) submission);
			SrlSubmission.Builder newSubmission = SrlSubmission.newBuilder();
			if (((SrlSolution) submission).hasSubmission()) { 
				SrlSubmission oldSubmission = ((SrlSolution) submission).getSubmission();
				if (oldSubmission.hasId()) {
					newSubmission.setId(oldSubmission.getId());
				}
			}
			newSubmission.setUpdateList(this.result.toByteString());
			newSubmission.setSubmissionTime(messageTime);
			return builder.setSubmission(newSubmission).build();
		}

		public SrlExperiment getExperiment() throws Exception {
			if (isSolution) {
				throw new Exception("Incorrect Message Type: cannot retrieve solution");
			}
			if (!this.isFinished()) {
				throw new Exception("cannot retrieve incomplete submission");
			}
			SrlExperiment.Builder builder = SrlExperiment.newBuilder((SrlExperiment) submission);
			builder.setUserId(userId);
			SrlSubmission.Builder newSubmission = SrlSubmission.newBuilder();
			if (((SrlExperiment) submission).hasSubmission()) { 
				SrlSubmission oldSubmission = ((SrlExperiment) submission).getSubmission();
				if (oldSubmission.hasId()) {
					newSubmission.setId(oldSubmission.getId());
				}
			}
			newSubmission.setUpdateList(this.result.toByteString());
			newSubmission.setSubmissionTime(messageTime);
			return builder.setSubmission(newSubmission).build();
		}
		
		public boolean hasId() {
			return hasId; // need to fix this
		}
		
		String getId() {
			return id;
		}
	}
}