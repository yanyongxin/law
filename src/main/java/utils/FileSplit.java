package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
/**
 * Split a large file into multiple files by specifying number of lines per file. Maximum 1000 files.
 * 
 * @author Yongxin
 *
 */
public class FileSplit {

	// split large file into multiple files.
	// line split, not byte split
	public static void main(String[] args) throws IOException {
		if(args.length != 3){
			System.out.println("args: infile outname lines");
			System.exit(1);
		}
		String infile = args[0];
		String outname = args[1];
		int nlines = Integer.valueOf(args[2]);
		
		String line;
		BufferedReader br = new BufferedReader(new FileReader(infile));
		boolean nomore = false;
		for(int i=0;i<1000;i++){// maximum 1000 files
			String outfile = outname + (i+1) + ".txt";
			BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
			for(int j=0;j<nlines;j++){
				line=br.readLine();
				if(line==null){
					nomore=true;
					break;
				}
				wr.write(line + "\n");
			}
			wr.close();
			if(nomore){
				break;
			}
		}
		br.close();
	}

}
