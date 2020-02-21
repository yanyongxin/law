package sftrack;

import static org.junit.Assert.fail;

import java.io.IOException;

//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertTrue;
//import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.conf.ConstraintJittingThresholdOption;
import org.kie.internal.io.ResourceFactory;

import sftrack.Ontology.Srunner;

public class SFcomplex {
	static Ontology onto;
	static final int TOP_N = 3;

	public static void main(String[] args) throws IOException {
		//		if (args.length != 2) {
		//			System.out.println("args: infile outfile");
		//			System.exit(1);
		//		}
		System.out.println("Initialization ...");
		try {
			System.out.println("Loading KieServices: ");
			KieServices ks = KieServices.Factory.get();
			System.out.println("Creating KieFileSystem: ");
			KieFileSystem kfs = ks.newKieFileSystem();
			System.out.println("Loading genome/thousand.drl: ");
			Resource dd = ResourceFactory.newClassPathResource("sftrack/thousand.drl");
			System.out.println("kfs.write sftrack/thousand.drl: ");
			kfs.write("src/main/resources/sftrack/thousand.drl", dd);
			KieBuilder kbuilder = ks.newKieBuilder(kfs);
			kbuilder.buildAll();
			KieContainer kcontainer = ks.newKieContainer(kbuilder.getKieModule().getReleaseId());
			KieBaseConfiguration kbConfig = KieServices.Factory.get().newKieBaseConfiguration();
			kbConfig.setOption(ConstraintJittingThresholdOption.get(-1));
			KieBase kbase = kcontainer.newKieBase(kbConfig);
			Ontology.LoadWorld("src/main/resources/sftrack/genericOnto.txt", "src/main/resources/sftrack/genericdict.txt");
			onto = Ontology.createOntology(kbase);
		} catch (Exception ex) {
			fail("Knowledge Base loading error!");
		}
		String testText = "Complaint against Comcast with jury demand by General Electric Company";
		String test1 = "MOTION TO STRIKE COMPLAINT FILED BY DEFENDANT HYUNDAI MOTOR AMERICA HEARING SET FOR JUL-18-2017 AT 02:00 PM IN DEPT 302";
		String test2 = "MEMORANDUM OF POINTS AND AUTHORITIES AND DEMURRER TO FIRST AMENDED COMPLAINT FILED BY DEFENDANT HYUNDAI MOTOR AMERICA";
		try {
			Srunner srun = onto.createSrunner(true);
			List<Phrase> phlist = CaseData.deToPhrases(test2, false);
			srun.insertList(phlist);
			srun.execute();
			Map<Integer, List<Phrase>> rpmap = srun.findAllPhrases();
			List<Integer> keylist = Analysis.buildKeyList(rpmap);
			//			assertTrue(keylist.size() > 0);
			keylist.add(phlist.size());
			ArrayList<Integer> segments = new ArrayList<Integer>();
			List<List<Analysis>> lla = Analysis.findBestNew(rpmap, keylist, TOP_N, segments);
			List<Phrase> plist = DocketEntry.getPhraseList(lla);
			//			assertNotNull("At least one parsed phrase", plist);
			//			assertEquals("Only one parsed phrase", plist.size(), 1);
			ERGraph g = plist.get(0).getGraph();
			//			Link lk1 = g.containLink("against", "DocLegalComplaint", null);
			//			//			assertNotNull("The against relation", lk1);
			//			Entity e1 = lk1.getArg2();
			//			//			assertTrue(e1.isInstanceOf("OrgCoComcast"));
			//			Link lk2 = g.containLink("with", "DocLegalComplaint", null);
			//			//			assertNotNull("The with relation", lk2);
			//			Entity e2 = lk2.getArg2();
			//			//			assertTrue(e2.isInstanceOf("ProcJuryDemand"));
			//			Link lk3 = g.containLink("byAgent", "DocLegalComplaint", null);
			//			//			assertNotNull("The byAgent relation", lk3);
			//			Entity e3 = lk3.getArg2();
			//			//			assertTrue(e3.isInstanceOf("OrgCoGeneralElectric"));
			srun.dispose();
		} catch (Exception ex) {
			fail(ex.getMessage());
		}

	}

}
