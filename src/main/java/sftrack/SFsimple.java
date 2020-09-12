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

import core.Analysis;
import core.ERGraph;
import core.LegaLanguage;
import core.Phrase;
import core.LegaLanguage.Srunner;

/**
 * Simplest program to invoke parser.
 * 
 * @author yanyo
 *
 */
public class SFsimple {
	static LegaLanguage onto;
	static final int TOP_N = 3;

	public static void main(String[] args) throws IOException {
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
			onto = LegaLanguage.create(kbase, "src/main/resources/sftrack/genericOnto.txt", "src/main/resources/sftrack/genericdict.txt");
		} catch (Exception ex) {
			fail("Knowledge Base loading error!");
		}
		String testText = "Complaint against Comcast with jury demand by General Electric Company";
		String test1 = "MOTION TO STRIKE COMPLAINT FILED BY DEFENDANT HYUNDAI MOTOR AMERICA HEARING SET FOR JUL-18-2017 AT 02:00 PM IN DEPT 302";
		String test2 = "MEMORANDUM OF POINTS AND AUTHORITIES AND DEMURRER TO FIRST AMENDED COMPLAINT FILED BY DEFENDANT HYUNDAI MOTOR AMERICA";
		String test3 = "WHOLE FOODS MARKET CALIFORNIA, INC.	Cross Defendant";
		String test4 = "LAFLEUR, LENA INDIVIDUALLY, AND ON BEHALF OF THE ESTATE OF GERVIS LAFLEUR, DECEASED";
		String test5 = "PFIZER, INC. A DELAWARE CORPORATION";
		String test6 = "";
		String test7 = "";
		try {
			Srunner srun = onto.createSrunner(true);
			List<Phrase> phlist = CaseData.deToPhrases(test4, false);
			srun.insertList(phlist);
			srun.execute();
			Map<Integer, List<Phrase>> rpmap = srun.findAllPhrases();
			List<Integer> keylist = Analysis.buildKeyList(rpmap);
			//			assertTrue(keylist.size() > 0);
			keylist.add(phlist.size());
			ArrayList<Integer> segments = new ArrayList<Integer>();
			List<List<Analysis>> lla = Analysis.findBestNew(rpmap, keylist, TOP_N, segments);
			List<Phrase> plist = DocketEntry.getPhraseList(lla);
			ERGraph g = plist.get(0).getGraph();
			srun.dispose();
		} catch (Exception ex) {
			fail(ex.getMessage());
		}

	}

}
