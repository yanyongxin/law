package com.sample;

import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
//import org.kie.api.runtime.KieContainer;
//import org.kie.api.runtime.KieSession;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

/**
 * This is a sample class to launch a rule.
 */
public class DroolsTest {

	public static final void main(String[] args) {
		try {
			KieServices ks = KieServices.Factory.get();
			KieFileSystem kfs = ks.newKieFileSystem();
			Resource dd = ResourceFactory.newClassPathResource("com/sample/rules/Sample.drl");
			kfs.write("src/main/resources/com/sample.rules/Sample.drl", dd);
			KieBuilder kbuilder = ks.newKieBuilder(kfs);
			kbuilder.buildAll();
			KieContainer kcontainer = ks.newKieContainer(kbuilder.getKieModule().getReleaseId());
			KieBase kbase = kcontainer.getKieBase();
			KieSession kSession = kbase.newKieSession();

			// go !
			Message message = new Message();
			message.setMessage("Hello World");
			message.setStatus(Message.HELLO);
			kSession.insert(message);
			kSession.fireAllRules();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static class Message {

		public static final int HELLO = 0;
		public static final int GOODBYE = 1;

		private String message;

		private int status;

		public String getMessage() {
			return this.message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public int getStatus() {
			return this.status;
		}

		public void setStatus(int status) {
			this.status = status;
		}

	}

}
