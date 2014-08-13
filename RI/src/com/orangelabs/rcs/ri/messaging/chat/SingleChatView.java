/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.ri.messaging.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
import com.gsma.services.rcs.chat.Chat;
import com.gsma.services.rcs.chat.ChatListener;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Smileys;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Single chat view
 */
public class SingleChatView extends ChatView {
	/**
	 * Intent parameters
	 */
	/* package private */ final static String EXTRA_CONTACT = "contact";

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(SingleChatView.class.getSimpleName());
	
	/**
	 * Remote contact
	 */
	private ContactId contact;
    
    /**
     * Chat 
     */
	private Chat chat;

    /**
     * Chat listener
     */
	private ChatListener chatListener = new ChatListener() {
		// Callback called when an Is-composing event has been received
		@Override
		public void onComposingEvent(final ContactId contact, final boolean status) {
			handler.post(new Runnable() {
				public void run() {
					TextView view = (TextView) findViewById(R.id.isComposingText);
					if (status) {
						// Display is-composing notification
						view.setText(getString(R.string.label_contact_is_composing, contact.toString()));
						view.setVisibility(View.VISIBLE);
					} else {
						// Hide is-composing notification
						view.setVisibility(View.GONE);
					}
				}
			});
		}

		@Override
		public void onMessageStatusChanged(ContactId contact, final String msgId, int status) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onMessageStatusChanged contact=" + contact + " msgId=" + msgId + " status=" + status);
			}
			if (status > MESSAGE_STATUSES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onMessageStatusChanged unhandled status=" + status);
				}
				return;
			}
			// TODO : handle reason code (CR025)
			int reasonCode = 0;
			String reason = (reasonCode == 0) ? "" : MESSAGE_REASON_CODES[0];
			final String notif = getString(R.string.label_message_status_changed, MESSAGE_STATUSES[status], reason);
			handler.post(new Runnable() {
				public void run() {
					addNotifHistory(notif, msgId);
				}
			});
		}

	};
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        processIntent(true);
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onCreate contact=" + contact);
		}
    }
    
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		// Replace the value of intent
		setIntent(intent);
		processIntent(false);
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onNewIntent contact=" + contact);
		}
	}
	
	/**
	 * Process intent
	 * @param loadHistory the history must be (re)loaded
	 */
	private void processIntent(boolean loadHistory) {
    	ChatMessageDAO messageDao = null;
    	if (getIntent() == null) {
    		if (LogUtils.isActive) {
				Log.e(LOGTAG, "processIntent invalid intent");
			}
    		return;
    	}
        int mode = getIntent().getIntExtra(SingleChatView.EXTRA_MODE, -1);
		switch (mode) {
		case ChatView.MODE_OPEN:
		case ChatView.MODE_OUTGOING:
			// Open chat
			contact = (ContactId) getIntent().getParcelableExtra(SingleChatView.EXTRA_CONTACT);
			loadHistory = true;
			break;

		case ChatView.MODE_INCOMING:
			// Get Incoming chat from its Intent
			messageDao = (ChatMessageDAO) (getIntent().getExtras()
					.getParcelable(ChatIntentService.BUNDLE_CHATMESSAGE_DAO_ID));
			if (messageDao == null || messageDao.getContact() == null) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "processIntent invalid chat message");
				}
				return;
			}
			// Are we switching to a new conversation ?
			if (!messageDao.getContact().equals(contact)) {
				// Force reload of history
				loadHistory = true;
				// Save new contactId
				contact = messageDao.getContact();
			}
			break;

		default:
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "processIntent invalid mode "+mode);
			}
			return;
		}
		keyChat = contact;
		// Set title
		setTitle(getString(R.string.title_chat) + " " +	contact);
		if (loadHistory) {
			// Activity is new or switch to new contact: load history
			unreadMessageIDs = loadHistory(contact.toString());
			if (serviceConnected) {
				try {
					for (String msgId : unreadMessageIDs) {
						chatApi.markMessageAsRead(msgId);
					}
					unreadMessageIDs.clear();
				} catch (JoynServiceException e) {
					if (LogUtils.isActive) {
						Log.e(LOGTAG, "processIntent failed to mark chat message as read");
					}
				}
			}
		} else {
			// Activity is created: display new message
			if (messageDao.getMimeType() == null|| messageDao.getBody() == null) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "processIntent invalid chat message");
				}
				return;
			}
			try {
				TextMessageItem message = new TextMessageItem(messageDao);
				// Add chat message to history
				addMessageHistory(message);
				if (serviceConnected) {
					try {
						chatApi.markMessageAsRead(messageDao.getMsgId());
					} catch (JoynServiceException e) {
						if (LogUtils.isActive) {
							Log.e(LOGTAG, "onNewIntent failed to mark chat message as read");
						}
					}
				} else {
					// Message will be marked as read upon connection
					unreadMessageIDs.add(message.getMessageId());
				}
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "processIntent failed to decode chat message", e);
				}
				return;
			}			
		}
	}
	
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successful):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
    	if (LogUtils.isActive) {
    		Log.d(LOGTAG, "onServiceConnected contact=" + contact);
    	}
    	try {
			// Set max label length
			int maxMsgLength = chatApi.getConfiguration().getSingleChatMessageMaxLength();
			if (maxMsgLength > 0) {
		        // Set the message composer max length
				InputFilter[] filterArray = new InputFilter[1];
				filterArray[0] = new InputFilter.LengthFilter(maxMsgLength);
				composeText.setFilters(filterArray);
			}
//			if (LogUtils.isActive) {
//				ChatServiceConfiguration conf = chatApi.getConfiguration();
//				Log.d(LOGTAG, "onServiceConnected GC min participants=" + conf.getGroupChatMinParticipants());
//				Log.d(LOGTAG, "onServiceConnected GC max participants=" + conf.getGroupChatMaxParticipants());
//				Log.d(LOGTAG, "onServiceConnected GC subject max length=" + conf.getGroupChatSubjectMaxLength());
//				Log.d(LOGTAG, "onServiceConnected is respond to display reports=" + conf.isRespondToDisplayReportsEnabled());
//			}
			// Add single chat event listener
			chatApi.addOneToOneChatEventListener(chatListener);
			
			serviceConnected = true;

			// Mark messages for this contact as read
			for (String msgId : unreadMessageIDs) {
				chatApi.markMessageAsRead(msgId);
			}
			unreadMessageIDs.clear();
			
			// Open chat
    		chat = chatApi.openSingleChat(contact);
							
			// Instantiate the composing manager
			composingManager = new IsComposingManager(chatApi.getConfiguration().getIsComposingTimeout() * 1000);
			
	    } catch(JoynServiceNotAvailableException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(SingleChatView.this, getString(R.string.label_api_disabled), exitOnce);
	    } catch(JoynServiceException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(SingleChatView.this, getString(R.string.label_api_failed), exitOnce);
		}
    }
    
    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
		serviceConnected = false;
		Utils.showMessageAndExit(SingleChatView.this, getString(R.string.label_api_disabled), exitOnce);
    }    

	/**
     * Send a text message
     * 
     * @param msg Message
     * @return Message ID
     */
    protected String sendTextMessage(String msg) {
    	try {
    		if (LogUtils.isActive) {
        		Log.d(LOGTAG, "sendTextMessage msg=" + msg+" chat="+chat);
        	}
			// Send the text to remote
			String msgId = chat.sendMessage(msg);
	        // Warn the composing manager that the message was sent
			composingManager.messageWasSent();
			return msgId;
	    } catch(Exception e) {
	    	e.printStackTrace();
	    	return null;
	    }
    }
    
    /**
     * Send geoloc message
     * 
     * @param geoloc Geoloc
     * @return Message ID
     */
    protected String sendGeolocMessage(Geoloc geoloc) {
        try {
			// Send the text to remote
        	String msgId = chat.sendGeoloc(geoloc);
	        // Warn the composing manager that the message was sent
	    	composingManager.messageWasSent();
	    	return msgId;
	    } catch(Exception e) {
	    	e.printStackTrace();
	    	return null;
	    }
    }
    
    /**
     * Quit the session
     */
    protected void quitSession() {
    	chat = null;
        // Exit activity
		finish();        
    }        	
        
    /**
     * Update the is composing status
     * 
     * @param isTyping Is composing status
     */
    protected void setTypingStatus(boolean isTyping) {
		try {
			if (chat != null) {
				chat.sendIsComposingEvent(isTyping);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}    
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = new MenuInflater(getApplicationContext());
		inflater.inflate(R.menu.menu_chat, menu);
		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_insert_smiley:
				Smileys.showSmileyDialog(
						this, 
						composeText, 
						getResources(), 
						getString(R.string.menu_insert_smiley));
				break;
	
			case R.id.menu_quicktext:
				addQuickText();
				break;

			case R.id.menu_send_geoloc:
				getGeoLoc();
				break;	
							
			case R.id.menu_showus_map:
				try {
					showUsInMap(chat.getRemoteContact());
			    } catch(JoynServiceException e) {
			    	e.printStackTrace();
					Utils.showMessageAndExit(SingleChatView.this, getString(R.string.label_api_failed), exitOnce);
				}
				break;	

			case R.id.menu_clear_log:
				// Delete conversation
				String where = ChatLog.Message.CHAT_ID + " = '" + contact + "'"; 
				getContentResolver().delete(ChatLog.Message.CONTENT_URI, where, null);
				
				// Refresh view
		        msgListAdapter = new MessageListAdapter(this);
		        setListAdapter(msgListAdapter);
				break;
		}
		return true;
	}
        
	@Override
	protected void removeServiceListener() {
		if (serviceConnected) {
			try {
				chatApi.removeOneToOneChatEventListener(chatListener);
			} catch (JoynServiceException e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "removeServiceListener failed",e);
				}
			}
		}
	}
}
