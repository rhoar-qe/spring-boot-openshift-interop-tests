package com.redhat.sample.resteasy.controller;

import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/")
@Component
public class RestApiController {

	@POST
	public String echo(@FormParam("text") String echoText) {
		return "echo:" + echoText;
	}

	@GET
	public String helloWorld(){
		return "Hello world!";
	}
}