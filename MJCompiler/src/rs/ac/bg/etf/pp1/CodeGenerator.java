package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGenerator extends VisitorAdaptor {
	private int mainPc;
	
	public int getMainPc() { return mainPc; }
	
	public void visit(PrintStatement printStatement) {
		if (printStatement.getExpr().struct == MyTab.intType) {
			Code.loadConst(5);
			Code.put(Code.print);
		} else {
			Code.loadConst(1);
			Code.put(Code.bprint);
		}
	}
	
	public void visit(FactorConst factorConst) {
		Obj con = MyTab.insert(Obj.Con, "$", factorConst.struct);
//		con.setAdr(factorConst.getConstValue());
	}
}
