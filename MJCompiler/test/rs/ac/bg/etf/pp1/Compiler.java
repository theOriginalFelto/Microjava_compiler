package rs.ac.bg.etf.pp1;

import rs.etf.pp1.mj.runtime.Run;
import rs.etf.pp1.mj.runtime.disasm;

public class Compiler {
	
	public static void main(String[] args) throws Exception {
		MJParserTest.main(args);

		if (MJParserTest.programOK) {
			System.out.println("===================================================");
			System.out.println("Kompajlirani Mikrojava kod:");
			System.out.println("===================================================");
			
			String[] objArg = {args[1]};
			disasm.main(objArg);
			Run.main(objArg);
		}
	}
}
