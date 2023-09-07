package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.CounterVisitor.FormParamCounter;
import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {
	String currentConstValueTypeName;
	int numberConstValue, boolConstValue;
	char charConstValue;
	
	private int mainPc;
	
	public int getMainPc() { return mainPc; }
	
	public boolean isMainMethod(String methodName, Struct methodType) {
		if (methodType == MyTab.noType && methodName.equals("main")) 
			return true;
		return false;
	}
	
	public void visit(PrintStatement printStatement) {
		if (printStatement.getExpr().struct == MyTab.intType) {
			Code.loadConst(5);
			Code.put(Code.print);
		} else if (printStatement.getExpr().struct == MyTab.charType) {
			Code.loadConst(1);
			Code.put(Code.bprint);
		} else {
			Code.loadConst(0);
			Code.put(Code.bprint);
		}
	}
	
	public void visit(FactorConst factorConst) {
		Obj constNode = MyTab.insert(Obj.Con, "$", factorConst.struct);
		constNode.setLevel(0);
		if (currentConstValueTypeName == "int")
    		constNode.setAdr(numberConstValue);
    	else if (currentConstValueTypeName == "char")
    		constNode.setAdr(charConstValue);
    	else
    		constNode.setAdr(boolConstValue);
		
		Code.load(constNode);
	}
	
	public void visit(ConstNumber cnst){
		cnst.struct = MyTab.intType;
		numberConstValue = cnst.getN1();
		currentConstValueTypeName = "int";
	}
	
	public void visit(ConstBoolean cnst){
		cnst.struct = MyTab.boolType;
		boolConstValue = cnst.getB1() ? 1 : 0;
		currentConstValueTypeName = "bool";
	}
	
	public void visit(ConstChar cnst){
		cnst.struct = MyTab.charType;
		charConstValue = cnst.getC1();
		currentConstValueTypeName = "char";
	}
	
	public void visit(MethodTypeDecl methodTypeDecl) {
		
	}
	
	public void visit(MethodVoidTypeDecl methodVoidTypeDecl) {
		if (methodVoidTypeDecl.getMethName().equals("main"))
			mainPc = Code.pc;
		methodVoidTypeDecl.obj.setAdr(Code.pc);
		// Collect arguments and local variables
		SyntaxNode methodNode = methodVoidTypeDecl.getParent();
		
		VarCounter varCnt = new VarCounter();
		methodNode.traverseTopDown(varCnt);
		
		FormParamCounter formParamCnt = new FormParamCounter();
		methodNode.traverseTopDown(formParamCnt);
		
		// Generate the entry
		Code.put(Code.enter);
		Code.put(formParamCnt.getCount());
		Code.put(formParamCnt.getCount() + varCnt.getCount());
	}
	
	public void visit(MethodDeclaration methodDeclaration) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
}
