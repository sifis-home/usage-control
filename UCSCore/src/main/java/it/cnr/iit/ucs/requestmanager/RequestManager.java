/*******************************************************************************
 * Copyright 2018 IIT-CNR
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package it.cnr.iit.ucs.requestmanager;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.ucs.message.Message;
import it.cnr.iit.ucs.message.attributechange.AttributeChangeMessage;
import it.cnr.iit.ucs.message.endaccess.EndAccessMessage;
import it.cnr.iit.ucs.message.reevaluation.ReevaluationResponseMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessMessage;
import it.cnr.iit.ucs.properties.components.RequestManagerProperties;
import it.cnr.iit.utility.errorhandling.Reject;

/**
 * All the requests coming to the context handler have to reach the request
 * manager first that will choose how to handle them.
 *
 * @author Antonio La Marra, Alessandro Rosetti
 */
public class RequestManager extends AbstractRequestManager {

	private static final Logger log = Logger.getLogger(RequestManager.class.getName());
	private boolean active = false;

	private ExecutorService inquirers;

	public RequestManager(RequestManagerProperties properties) {
		super(properties);
		this.active = properties.isActive();
		initializeInquirers();
	}

	/**
	 * Initialises the request manager with a pool of threads
	 *
	 * @return true if everything goes fine, false in case of exceptions
	 */
	private void initializeInquirers() {
		try {
			inquirers = Executors.newFixedThreadPool(1);
		} catch (Exception e) {
			log.severe("Error initialising the RequestManager inquirers : " + e.getMessage());
		}
	}

	@Override
	public synchronized void sendReevaluation(ReevaluationResponseMessage reevaluation) {
		Reject.ifNull(reevaluation, "Null message");
		log.info("Sending on going reevaluation.");
		getPEPMap().get((reevaluation).getPepId()).onGoingEvaluation(reevaluation);
	}

	/**
	 * Handles the case of a message received from outside Once a message coming
	 * from outside is received from the request manager, it puts it in the priority
	 * queue of messages
	 */
	@Override
	public synchronized Message sendMessage(Message message) {
		Reject.ifNull(message, "Null message");
		try {
			if (!active) {
				System.out.println("\n\n\n NOT ACTIVE \n\n\n");
				return handleMessage(message);
			} else {
				System.out.println("\n\n\n ACTIVE \n\n\n");
				getQueueOutput().put(message);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.severe("e.getLocalizedMessage()=" + e.getLocalizedMessage());
			Thread.currentThread().interrupt();
		}
		return null;
	}

	/**
	 * The context handler inquirers perform an infinite loop in order to retrieve
	 * the messages coming to the request manager and sends them to the context
	 * handler.
	 */
	private class ContextHandlerInquirer implements Callable<Message> {

		@Override
		public Message call() {
			Message message;
			try {
				while ((message = getQueueOutput().take()) != null) {
					handleMessage(message);
				}
			} catch (Exception e) {
				log.severe(e.getMessage());
				Thread.currentThread().interrupt();
			}
			return null;
		}
	}

	private Message handleMessage(Message message) throws Exception {
		Message responseMessage = null;
		if (message instanceof AttributeChangeMessage) {
			getContextHandler().attributeChanged((AttributeChangeMessage) message);
			System.out.println("\n\n\n first if \n\n\n");
			return null;
		} else if (message.getPurpose() == PURPOSE.TRY) {
			System.out.println("\n\n\n try if \n\n\n");
			responseMessage = getContextHandler().tryAccess((TryAccessMessage) message);
			if (responseMessage == null) {
				log.severe("\n\n\nresponseMessage is null\n\n\n");
			}
		} else if (message.getPurpose() == PURPOSE.START) {
			System.out.println("\n\n\n start if \n\n\n");
			responseMessage = getContextHandler().startAccess((StartAccessMessage) message);
		} else if (message.getPurpose() == PURPOSE.END) {
			System.out.println("\n\n\n end if \n\n\n");
			responseMessage = getContextHandler().endAccess((EndAccessMessage) message);
		} else {
			System.out.println("\n\n\n catch if \n\n\n");
			throw new IllegalArgumentException("Invalid message arrived");
		}
		if (active) {
			getPEPMap().get(responseMessage.getDestination()).receiveResponse(responseMessage);
		}

		System.out.println("responseMessage.toString()=" + responseMessage.toString());
		return responseMessage;
	}

	@Override
	public void startMonitoring() {
		inquirers.submit(new ContextHandlerInquirer());
	}

}
