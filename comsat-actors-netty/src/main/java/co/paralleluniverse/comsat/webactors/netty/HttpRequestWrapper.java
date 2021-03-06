/*
 * COMSAT
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.comsat.webactors.netty;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.comsat.webactors.Cookie;
import co.paralleluniverse.comsat.webactors.HttpRequest;
import co.paralleluniverse.comsat.webactors.HttpResponse;
import co.paralleluniverse.comsat.webactors.WebMessage;
import com.google.common.collect.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;

/**
 * @author circlespainter
 */
final class HttpRequestWrapper extends HttpRequest {
	public static final String CHARSET_MARKER_STRING = "charset=";

	private final ByteBuf reqContent;

	final ActorRef<? super HttpResponse> actorRef;
	final FullHttpRequest req;
	final ChannelHandlerContext ctx;

	private ImmutableMultimap<String, String> params;
	private URI uri;
	private Collection<Cookie> cookies;
	private ListMultimap<String, String> heads;
	private ByteBuffer byteBufferBody;
	private String stringBody;
	private Charset encoding;
	private String contentType;

	public HttpRequestWrapper(ActorRef<? super HttpResponse> actorRef, ChannelHandlerContext ctx, FullHttpRequest req) {
		this.actorRef = actorRef;
		this.ctx = ctx;
		this.req = req;
		this.reqContent = Unpooled.copiedBuffer(req.content());
	}

	@Override
	public Multimap<String, String> getParameters() {
		QueryStringDecoder queryStringDecoder;
		if (params == null) {
			queryStringDecoder = new QueryStringDecoder(req.getUri());
			final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
			final Map<String, List<String>> parameters = queryStringDecoder.parameters();
			for (final String k : parameters.keySet())
				builder.putAll(k, parameters.get(k));
			params = builder.build();
		}
		return params;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return ImmutableMap.of(); // No attributes in Netty; Guava's impl. will return a pre-built instance
	}

	@Override
	public String getScheme() {
		initUri();
		return uri.getScheme();
	}

	private void initUri() {
		if (uri == null) {
			try {
				uri = new URI(req.getUri());
			} catch (final URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public String getMethod() {
		return req.getMethod().name();
	}

	@Override
	public String getPathInfo() {
		initUri();
		return uri.getPath();
	}

	@Override
	public String getContextPath() {
		return "/"; // Context path makes sense only for servlets
	}

	@Override
	public String getQueryString() {
		initUri();
		return uri.getQuery();
	}

	@Override
	public String getRequestURI() {
		return req.getUri();
	}

	@Override
	public String getServerName() {
		initUri();
		return uri.getHost();
	}

	@Override
	public int getServerPort() {
		initUri();
		return uri.getPort();
	}

	@SuppressWarnings("unchecked")
	@Override
	public ActorRef<WebMessage> getFrom() {
		return (ActorRef<WebMessage>) actorRef;
	}

	@Override
	public ListMultimap<String, String> getHeaders() {
		if (heads == null) {
			heads = extractHeaders(req.headers());
		}
		return heads;
	}

	@Override
	public Collection<Cookie> getCookies() {
		if (cookies == null) {
			final ImmutableList.Builder<Cookie> builder = ImmutableList.builder();
			for (io.netty.handler.codec.http.cookie.Cookie c : ServerCookieDecoder.LAX.decode(req.headers().get("Cookies"))) {
				builder.add(
					Cookie.cookie(c.name(), c.value())
						.setDomain(c.domain())
						.setPath(c.path())
						.setHttpOnly(c.isHttpOnly())
						.setMaxAge((int) c.maxAge())
						.setSecure(c.isSecure())
						.build()
				);
			}
			cookies = builder.build();
		}
		return cookies;
	}

	@Override
	public int getContentLength() {
		final String stringBody = getStringBody();
		if (stringBody != null)
			return stringBody.length();
		final ByteBuffer bufferBody = getByteBufferBody();
		if (bufferBody != null)
			return bufferBody.remaining();
		return 0;
	}

	@Override
	public Charset getCharacterEncoding() {
		if (encoding == null)
			encoding = extractCharacterEncoding(getHeaders());
		return encoding;
	}

	@Override
	public String getContentType() {
		if (contentType == null) {
			getHeaders();
			if (heads != null) {
				final List<String> cts = heads.get(CONTENT_TYPE);
				if (cts != null && cts.size() > 0)
					contentType = cts.get(0);
			}
		}
		return null;
	}

	@Override
	public String getStringBody() {
		if (stringBody == null) {
			if (byteBufferBody != null)
				return null;
			decodeStringBody();
		}
		return stringBody;
	}

	@Override
	public ByteBuffer getByteBufferBody() {
		if (byteBufferBody == null) {
			if (stringBody != null)
				return null;
			if (reqContent != null)
				byteBufferBody = reqContent.nioBuffer();
		}
		return byteBufferBody;
	}

	private String decodeStringBody() {
		if (reqContent != null) {
			try {
				stringBody =
					getCharsetEncodingOrDefault()
						.newDecoder()
						.onMalformedInput(CodingErrorAction.REPORT)
						.onUnmappableCharacter(CodingErrorAction.REPORT)
						.decode(reqContent.nioBuffer())
						.toString();
			} catch (CharacterCodingException ignored) {
			}
		}
		return stringBody;
	}

	Charset getCharsetEncodingOrDefault() {
		return getOrDefault(getCharacterEncoding());
	}

	static ImmutableListMultimap<String, String> extractHeaders(HttpHeaders headers) {
		if (headers != null) {
			final ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
			for (final String n : headers.names())
				builder.putAll(n, headers.getAll(n));
			return builder.build();
		}
		return null;
	}

	static Charset extractCharacterEncoding(ListMultimap<String, String> heads) {
		if (heads != null) {
			final List<String> cts = heads.get(CONTENT_TYPE);
			if (cts != null && cts.size() > 0) {
				final String ct = cts.get(0).trim().toLowerCase();
				if (ct.contains(CHARSET_MARKER_STRING)) {
					try {
						return Charset.forName(ct.substring(ct.indexOf(CHARSET_MARKER_STRING) + CHARSET_MARKER_STRING.length()).trim());
					} catch (UnsupportedCharsetException ignored) {
					}
				}
			}
		}
		return null;
	}

	static Charset extractCharacterEncodingOrDefault(HttpHeaders headers) {
		return getOrDefault(extractCharacterEncoding(extractHeaders(headers)));
	}

	private static Charset getOrDefault(Charset characterEncoding) {
		if (characterEncoding == null)
			return Charset.defaultCharset();
		return characterEncoding;
	}
}
