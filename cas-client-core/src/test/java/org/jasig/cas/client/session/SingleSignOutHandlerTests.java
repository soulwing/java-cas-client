/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.cas.client.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.jasig.cas.client.http.servlet.DelegatingClientSession;
import org.jasig.cas.client.http.servlet.DelegatingHttpRequest;
import org.jasig.cas.client.http.servlet.DelegatingHttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

/**
 * @author Matt Brown <matt.brown@citrix.com>
 * @version $Revision$ $Date$
 * @since 3.2.1
 */
public final class SingleSignOutHandlerTests {

    private final static String ANOTHER_PARAMETER = "anotherParameter";
    private final static String TICKET = "ST-xxxxxxxx";
    private final static String URL = "http://mycasserver";
    private final static String LOGOUT_PARAMETER_NAME = "logoutRequest2";
    private final static String FRONT_LOGOUT_PARAMETER_NAME = "SAMLRequest2";
    private final static String RELAY_STATE_PARAMETER_NAME = "RelayState2";
    private final static String ARTIFACT_PARAMETER_NAME = "ticket2";

    private SingleSignOutHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        handler = new SingleSignOutHandler();
        handler.setLogoutParameterName(LOGOUT_PARAMETER_NAME);
        handler.setFrontLogoutParameterName(FRONT_LOGOUT_PARAMETER_NAME);
        handler.setRelayStateParameterName(RELAY_STATE_PARAMETER_NAME);
        handler.setArtifactParameterName(ARTIFACT_PARAMETER_NAME);
        handler.setCasServerUrlPrefix(URL);
        handler.init();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    public void tokenRequestFailsIfNoSession() {
        handler.setEagerlyCreateSessions(false);
        request.setSession(null);
        request.setParameter(ARTIFACT_PARAMETER_NAME, TICKET);
        request.setQueryString(ARTIFACT_PARAMETER_NAME + "=" + TICKET);
        assertTrue(handler.process(new DelegatingHttpRequest(request), new DelegatingHttpResponse(response)));
        final SessionMappingStorage storage = handler.getSessionMappingStorage();
        assertNull(storage.removeSessionByMappingId(TICKET));
    }

    @Test
    public void tokenRequestFailsIfBadParameter() {
        final MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        request.setParameter(ANOTHER_PARAMETER, TICKET);
        request.setQueryString(ANOTHER_PARAMETER + "=" + TICKET);
        assertTrue(handler.process(new DelegatingHttpRequest(request), new DelegatingHttpResponse(response)));
        final SessionMappingStorage storage = handler.getSessionMappingStorage();
        assertNull(storage.removeSessionByMappingId(TICKET));
    }

    @Test
    public void tokenRequestOK() {
        final MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        request.setParameter(ARTIFACT_PARAMETER_NAME, TICKET);
        request.setQueryString(ARTIFACT_PARAMETER_NAME + "=" + TICKET);
        assertTrue(handler.process(new DelegatingHttpRequest(request), new DelegatingHttpResponse(response)));
        final SessionMappingStorage storage = handler.getSessionMappingStorage();
        assertEquals(session.getId(), storage.removeSessionByMappingId(TICKET).getId());
    }

    @Test
    public void backChannelLogoutFailsIfMultipart() {
        final String logoutMessage = LogoutMessageGenerator.generateBackChannelLogoutMessage(TICKET);
        request.setParameter(LOGOUT_PARAMETER_NAME, logoutMessage);
        request.setMethod("POST");
        request.setContentType("multipart/form-data");
        final MockHttpSession session = new MockHttpSession();
        handler.getSessionMappingStorage().addSessionById(TICKET, new DelegatingClientSession(session));
        assertTrue(handler.process(new DelegatingHttpRequest(request), new DelegatingHttpResponse(response)));
        assertFalse(session.isInvalid());
    }

    @Test
    public void backChannelLogoutFailsIfNoSessionIndex() {
        final String logoutMessage = LogoutMessageGenerator.generateBackChannelLogoutMessage("");
        request.setParameter(LOGOUT_PARAMETER_NAME, logoutMessage);
        request.setMethod("POST");
        final MockHttpSession session = new MockHttpSession();
        handler.getSessionMappingStorage().addSessionById(TICKET, new DelegatingClientSession(session));
        assertFalse(handler.process(new DelegatingHttpRequest(request), new DelegatingHttpResponse(response)));
        assertFalse(session.isInvalid());
    }

    @Test
    public void backChannelLogoutOK() {
        final String logoutMessage = LogoutMessageGenerator.generateBackChannelLogoutMessage(TICKET);
        request.setParameter(LOGOUT_PARAMETER_NAME, logoutMessage);
        request.setMethod("POST");
        final MockHttpSession session = new MockHttpSession();
        handler.getSessionMappingStorage().addSessionById(TICKET, new DelegatingClientSession(session));
        assertFalse(handler.process(new DelegatingHttpRequest(request), new DelegatingHttpResponse(response)));
        assertTrue(session.isInvalid());
    }

    @Test
    public void frontChannelLogoutFailsIfBadParameter() {
        final String logoutMessage = LogoutMessageGenerator.generateFrontChannelLogoutMessage(TICKET);
        request.setParameter(ANOTHER_PARAMETER, logoutMessage);
        request.setMethod("GET");
        request.setQueryString(ANOTHER_PARAMETER + "=" + logoutMessage);
        final MockHttpSession session = new MockHttpSession();
        handler.getSessionMappingStorage().addSessionById(TICKET, new DelegatingClientSession(session));
        assertTrue(handler.process(new DelegatingHttpRequest(request), new DelegatingHttpResponse(response)));
        assertFalse(session.isInvalid());
    }

    @Test
    public void frontChannelLogoutFailsIfNoSessionIndex() {
        final String logoutMessage = LogoutMessageGenerator.generateFrontChannelLogoutMessage("");
        request.setParameter(FRONT_LOGOUT_PARAMETER_NAME, logoutMessage);
        request.setQueryString(FRONT_LOGOUT_PARAMETER_NAME + "=" + logoutMessage);
        request.setMethod("GET");
        final MockHttpSession session = new MockHttpSession();
        handler.getSessionMappingStorage().addSessionById(TICKET, new DelegatingClientSession(session));
        assertFalse(handler.process(new DelegatingHttpRequest(request), new DelegatingHttpResponse(response)));
        assertFalse(session.isInvalid());
    }

    @Test
    public void frontChannelLogoutOK() {
        final String logoutMessage = LogoutMessageGenerator.generateFrontChannelLogoutMessage(TICKET);
        request.setParameter(FRONT_LOGOUT_PARAMETER_NAME, logoutMessage);
        request.setQueryString(FRONT_LOGOUT_PARAMETER_NAME + "=" + logoutMessage);
        request.setMethod("GET");
        final MockHttpSession session = new MockHttpSession();
        handler.getSessionMappingStorage().addSessionById(TICKET, new DelegatingClientSession(session));
        assertFalse(handler.process(new DelegatingHttpRequest(request), new DelegatingHttpResponse(response)));
        assertTrue(session.isInvalid());
        assertNull(response.getRedirectedUrl());
    }

    @Test
    public void frontChannelLogoutRelayStateOK() {
        final String logoutMessage = LogoutMessageGenerator.generateFrontChannelLogoutMessage(TICKET);
        request.setParameter(FRONT_LOGOUT_PARAMETER_NAME, logoutMessage);
        request.setParameter(RELAY_STATE_PARAMETER_NAME, TICKET);
        request.setQueryString(FRONT_LOGOUT_PARAMETER_NAME + "=" + logoutMessage + "&" + RELAY_STATE_PARAMETER_NAME + "=" + TICKET);
        request.setMethod("GET");
        final MockHttpSession session = new MockHttpSession();
        handler.getSessionMappingStorage().addSessionById(TICKET, new DelegatingClientSession(session));
        assertFalse(handler.process(new DelegatingHttpRequest(request), new DelegatingHttpResponse(response)));
        assertTrue(session.isInvalid());
        assertEquals(URL + "/logout?_eventId=next&" + RELAY_STATE_PARAMETER_NAME + "=" + TICKET,
                response.getRedirectedUrl());
    }
}
