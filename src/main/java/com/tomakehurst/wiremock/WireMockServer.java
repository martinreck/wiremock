package com.tomakehurst.wiremock;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;

import com.tomakehurst.wiremock.common.FileSource;
import com.tomakehurst.wiremock.common.SingleRootFileSource;
import com.tomakehurst.wiremock.mapping.AdminRequestHandler;
import com.tomakehurst.wiremock.mapping.InMemoryMappings;
import com.tomakehurst.wiremock.mapping.Mappings;
import com.tomakehurst.wiremock.mapping.MockServiceRequestHandler;
import com.tomakehurst.wiremock.mapping.RequestHandler;
import com.tomakehurst.wiremock.servlet.FileBodyLoadingResponseRenderer;
import com.tomakehurst.wiremock.servlet.HandlerDispatchingServlet;
import com.tomakehurst.wiremock.servlet.ResponseRenderer;
import com.tomakehurst.wiremock.standalone.JsonFileMappingsLoader;
import com.tomakehurst.wiremock.standalone.MappingsLoader;
import com.tomakehurst.wiremock.verification.InMemoryRequestJournal;

public class WireMockServer {

	private static final int DEFAULT_PORT = 8080;
	private static final int PORT_NUMBER_ARG = 0;
	
	private Server jettyServer;
	private Mappings mappings;
	private InMemoryRequestJournal requestJournal;
	private RequestHandler mockServiceRequestHandler;
	private RequestHandler mappingRequestHandler;
	private FileBodyLoadingResponseRenderer responseRenderer;
	private int port;
	
	public WireMockServer(int port, FileSource bodyFileSource) {
		mappings = new InMemoryMappings();
		requestJournal = new InMemoryRequestJournal();
		mockServiceRequestHandler = new MockServiceRequestHandler(mappings);
		mockServiceRequestHandler.addRequestListener(requestJournal);
		mappingRequestHandler = new AdminRequestHandler(mappings, requestJournal);
		responseRenderer = new FileBodyLoadingResponseRenderer(bodyFileSource);
		this.port = port;
	}
	
	public WireMockServer(int port) {
		this(port, new SingleRootFileSource("src/test/resources"));
	}
	
	public WireMockServer() {
		this(DEFAULT_PORT);
	}
	
	public void start() {
		startMockServiceAndAdminServers();
	}
	
	public void stop() {
		try {
			jettyServer.stop();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void startMockServiceAndAdminServers() {
		jettyServer = new Server(port);
		
		Context adminContext = new Context(jettyServer, "/__admin");
		adminContext.addServlet(HandlerDispatchingServlet.class, "/");
		adminContext.setAttribute(RequestHandler.CONTEXT_KEY, mappingRequestHandler);
		adminContext.setAttribute(ResponseRenderer.CONTEXT_KEY, responseRenderer);
		jettyServer.addHandler(adminContext);
		
		Context mockServiceContext = new Context(jettyServer, "/");
		mockServiceContext.setAttribute(RequestHandler.CONTEXT_KEY, mockServiceRequestHandler);
		mockServiceContext.setAttribute(ResponseRenderer.CONTEXT_KEY, responseRenderer);
		mockServiceContext.addServlet(HandlerDispatchingServlet.class, "/");
		jettyServer.addHandler(mockServiceContext);

		try {
			jettyServer.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void loadMappingsUsing(MappingsLoader mappingsLoader) {
		mappingsLoader.loadMappingsInto(mappings);
	}
	
	public void setBodyFileSource(FileSource fileSource) {
		
	}
	
	public static void main(String... args) {
		FileSource bodyFileSource = new SingleRootFileSource("files");
		
		WireMockServer wireMockServer;
		if (args.length > 0) {
			int port = Integer.parseInt(args[PORT_NUMBER_ARG]);
			wireMockServer = new WireMockServer(port, bodyFileSource);
		} else {
			wireMockServer = new WireMockServer(DEFAULT_PORT, bodyFileSource);
		}
		
		MappingsLoader mappingsLoader = new JsonFileMappingsLoader("mappings");
		wireMockServer.loadMappingsUsing(mappingsLoader);
		wireMockServer.start();
	}
}
