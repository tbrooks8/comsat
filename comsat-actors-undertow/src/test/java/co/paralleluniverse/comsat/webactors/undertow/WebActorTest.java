/*
 * COMSAT
 * Copyright (C) 2014-2015, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.comsat.webactors.undertow;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorImpl;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.comsat.webactors.AbstractWebActorTest;
import co.paralleluniverse.comsat.webactors.WebMessage;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.RequestDumpingHandler;
import io.undertow.server.session.SessionCookieConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;

@RunWith(Parameterized.class)
public class WebActorTest extends AbstractWebActorTest {
	private static final Actor actor = new UndertowWebActor();
	@SuppressWarnings("unchecked")
	private static final ActorRef<? extends WebMessage> actorRef = actor.spawn();

	private static final Callable<WebActorHandler> basicWebActorHandlerCreator = new Callable<WebActorHandler>() {
		@Override
		public WebActorHandler call() throws Exception {
			return new WebActorHandler(new WebActorHandler.ContextProvider() {
				@Override
				public WebActorHandler.Context get(HttpServerExchange xch) {
					return new WebActorHandler.DefaultContextImpl() {
						@SuppressWarnings("unchecked")
						@Override
						public ActorRef<? extends WebMessage> getRef() {
							return actorRef;
						}

						@Override
						public Class<? extends ActorImpl<? extends WebMessage>> getWebActorClass() {
							return UndertowWebActor.class;
						}
					};
				}
			});
		}
	};

	private static final Callable<WebActorHandler> autoWebActorHandlerCreator = new Callable<WebActorHandler>() {
		@Override
		public WebActorHandler call() throws Exception {
			return new AutoWebActorHandler();
		}
	};

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{basicWebActorHandlerCreator},
				{autoWebActorHandlerCreator}
		});
	}

	private static final int INET_PORT = 8080;

	private final Callable<WebActorHandler> webActorHandlerCreator;

	private Undertow server;

	public WebActorTest(Callable<WebActorHandler> webActorHandlerCreator) {
		this.webActorHandlerCreator = webActorHandlerCreator;
	}

	@Before
	public void setUp() throws Exception {
		server = Undertow.builder()
				.addHttpListener(INET_PORT, "localhost")
				.setHandler(new RequestDumpingHandler(webActorHandlerCreator.call())).build();
		server.start();

		System.out.println("Server is up");
	}

	@After
	public void tearDown() throws Exception {
		server.stop();

		System.out.println("Server is down");
	}

	@Override
	protected String getSessionIdCookieName() {
		return SessionCookieConfig.DEFAULT_SESSION_ID;
	}
}
