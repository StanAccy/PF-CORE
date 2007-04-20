package de.dal33t.powerfolder.util.task;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;

/**
 * This task tries to send an invitation to another user.
 * The task remains active even when PF is closed and will try to
 * perform it's work on the next PF session.
 * 
 * @author Dennis "Bytekeeper" Waldherr </a>
 * @version $Revision$
 */
public class SendInvitationTask extends PersistentTask {
	private static final long serialVersionUID = 1L;
	private Invitation invitation;
	private MemberInfo target;
	
	private transient NodeManagerListener listener; 
	
	public SendInvitationTask(Invitation invitation, MemberInfo target) {
		this.invitation = invitation;
		this.target = target;
	}
	
	@Override
	public void init(PersistentTaskManager handler) {
		super.init(handler);
		listener = new InvitationTrigger();
		// Try to execute the task immediatly
		if (!execute()) {
			getController().getNodeManager().addNodeManagerListener(listener);
			Member node = target.getNode(getController());
			if (node != null) {
				node.markForImmediateConnect();
			}
		}
	}

	private boolean execute() {
		Member node = target.getNode(getController());
		if (node != null && node.isCompleteyConnected()) {
			node.sendMessageAsynchron(
					invitation, "Failed to send invitation!");
			remove();
			return true;
		}
		return false;
	}

	@Override
	public void shutdown() {
		if (getController() != null && getController().getNodeManager() != null) {
			getController().getNodeManager().removeNodeManagerListener(listener);
		}
		super.shutdown();
	}

	/**
	 * @return this task's invititation message.
	 */
	public Invitation getInvitation() {
		return invitation;
	}

	/**
	 * @return the target user who should receive the invitation.
	 */
	public MemberInfo getTarget() {
		return target;
	}
	
	private class InvitationTrigger implements NodeManagerListener {
		public void friendAdded(NodeManagerEvent e) { }
		public void friendRemoved(NodeManagerEvent e) { }

		public void nodeAdded(NodeManagerEvent e) { }

		public void nodeConnected(NodeManagerEvent e) {
			if (target.matches(e.getNode())) {
				execute();
			}
		}
		
		public void nodeDisconnected(NodeManagerEvent e) { }

		public void nodeRemoved(NodeManagerEvent e) { }

		public void settingsChanged(NodeManagerEvent e) { }

		public boolean fireInEventDispathThread() { 
			return false;
		}
	}
}
