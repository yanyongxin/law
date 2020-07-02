package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PACER_entity_filter {
	static List<EntityCase> atts;
	static List<EntityCase> jdgs;
	static List<EntityCase> ptys;
	static List<String> lookIDs = new ArrayList<>();

	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			System.out.println("args: inA inJ inP outfile caseID ");
			System.exit(-1);
		}
		String inA = args[0];
		String inJ = args[1];
		String inP = args[2];
		String outfile = args[3];
		for (int i = 4; i < args.length; i++) {
			lookIDs.add(args[i]);
		}
		atts = loadEntity(inA);
		jdgs = loadEntity(inJ);
		ptys = loadEntity(inP);
	}

	static List<EntityCase> loadEntity(String infile) throws IOException {
		List<EntityCase> ets = new ArrayList<>();
		BufferedReader br = new BufferedReader(new FileReader(infile));
		String line;
		while ((line = br.readLine()) != null) {
			String[] items = line.split("\\t");
			if (items[0].startsWith("rc")) {
				continue;
			}
			String[] ids = items[2].split("\\s+");
			for (String id : ids) {
				if (lookIDs.contains(id)) {

				}
			}
		}
		br.close();
		return ets;
	}

	static class EntityCase {
		String caseID;
		String entityName;

		public EntityCase(String cID, String name) {
			caseID = cID;
			entityName = name;
		}
	}
}
