package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.CounterVisitor.FormParamCounter;
import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {
	String currentConstValueTypeName;
	int numberConstValue;
	boolean boolConstValue;
	char charConstValue;
	
	private Obj[] printTrue = {
			new Obj(Obj.Con, "$", MyTab.charType),
			new Obj(Obj.Con, "$", MyTab.charType),
			new Obj(Obj.Con, "$", MyTab.charType),
			new Obj(Obj.Con, "$", MyTab.charType)
	};
	private Obj[] printFalse = {
			new Obj(Obj.Con, "$", MyTab.charType),
			new Obj(Obj.Con, "$", MyTab.charType),
			new Obj(Obj.Con, "$", MyTab.charType),
			new Obj(Obj.Con, "$", MyTab.charType),
			new Obj(Obj.Con, "$", MyTab.charType)
	};
	private Obj designatorObj;
	private Obj findAnyBoolVarDesignatorObj, findAnyArrayDesignatorObj;
	private int findAnyDesignatorCounter;
	
	private boolean storingArrayElement = false;
	
	private int mainPc;
	
	public CodeGenerator() {
		printTrue[0].setAdr('t');
		printTrue[1].setAdr('r');
		printTrue[2].setAdr('u');
		printTrue[3].setAdr('e');
		
		printFalse[0].setAdr('f');
		printFalse[1].setAdr('a');
		printFalse[2].setAdr('l');
		printFalse[3].setAdr('s');
		printFalse[4].setAdr('e');
	}
	
	public int getMainPc() { return mainPc; }
	
	public void storeExpr(Struct type) {
		Code.put(Code.putstatic);
		if (type == MyTab.intType)
			Code.put2(0);
		else if (type == MyTab.charType)
			Code.put2(1);
		else
			Code.put2(2);			
	}
	public void loadExpr(Struct type) {
		Code.put(Code.getstatic);
		if (type == MyTab.intType)
			Code.put2(0);
		else if (type == MyTab.charType)
			Code.put2(1);
		else
			Code.put2(2);
	}
	
	public void boolPrint() {
		Code.loadConst(32);
		Code.loadConst(1);
		Code.put(Code.bprint);
		
		Code.loadConst(1);
		Code.put(Code.jcc + Code.ne);
		Code.put2(7 * printTrue.length + 3 + 3);
		for (Obj obj : printTrue) {
			Code.load(obj);
			Code.loadConst(1);
			Code.put(Code.bprint);
		}
		Code.put(Code.jmp); Code.put2(7 * printFalse.length + 3);
		for (Obj obj : printFalse) {
			Code.load(obj);
			Code.loadConst(1);
			Code.put(Code.bprint);
		}
		
		Code.loadConst(32);
		Code.loadConst(1);
		Code.put(Code.bprint);
	}
	
	// Visit methods
	
	public void visit(MethodTypeDecl methodTypeDecl) {
		methodTypeDecl.obj.setAdr(Code.pc);
		// Collect arguments and local variables
		SyntaxNode methodNode = methodTypeDecl.getParent();
		
		VarCounter varCnt = new VarCounter();
		methodNode.traverseTopDown(varCnt);
		
		FormParamCounter formParamCnt = new FormParamCounter();
		methodNode.traverseTopDown(formParamCnt);
		
		// Generate the entry
		Code.put(Code.enter);
		Code.put(formParamCnt.getCount());
		Code.put(formParamCnt.getCount() + varCnt.getCount());
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
	
	
	public void visit(AssignStatement assignStatement) {
		Obj o = assignStatement.getDesignator().obj;
		if (storingArrayElement) {
			if (o.getType().getElemType() == MyTab.intType)
				Code.put(Code.astore);
			else
				Code.put(Code.bastore);
		} else
			Code.store(o);
	}
	
	public void visit(IncrementStatement incrementStatement) {
		Obj o = incrementStatement.getDesignator().obj;
		Code.loadConst(1);
		Code.put(Code.add);
		if (o.getType().getElemType() != null) {
			if (o.getType().getElemType() == MyTab.intType)
				Code.put(Code.astore);
			else
				Code.put(Code.bastore);
		} else 
			Code.store(o);
	}
	public void visit(DecrementStatement decrementStatement) {
		Obj o = decrementStatement.getDesignator().obj;
		Code.loadConst(-1);
		Code.put(Code.add);
		if (o.getType().getElemType() != null) {
			if (o.getType().getElemType() == MyTab.intType)
				Code.put(Code.astore);
			else
				Code.put(Code.bastore);
		} else 
			Code.store(o);
	}
	
	public void visit(ReturnNoExprStatement returnNoExprStatement) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	public void visit(ReturnExprStatement returnExprStatement) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(PrintStatement printStatement) {
		if (printStatement.getExpr().struct == MyTab.intType) {
			Code.loadConst(5);
			Code.put(Code.print);
		} else if (printStatement.getExpr().struct == MyTab.charType) {
			Code.loadConst(1);
			Code.put(Code.bprint);
		} else {
			boolPrint();
		}
	}
	public void visit(PrintStatementNumber printStatementNumber) {
		int width = printStatementNumber.getNumVal();
		if (printStatementNumber.getExpr().struct == MyTab.intType) {
			Code.loadConst(width);
			Code.put(Code.print);
		} else if (printStatementNumber.getExpr().struct == MyTab.charType) {
			Code.loadConst(width);
			Code.put(Code.bprint);
		} else {
			boolPrint();
		}
	}
	public void visit(ReadStatement readStatement) {
		Obj o = readStatement.getDesignator().obj;
		if (o.getType().getElemType() != null) {
			if (o.getType().getElemType() == MyTab.intType) {
				Code.put(Code.read);
				Code.put(Code.astore);
			}
			else {
				Code.put(Code.bread);
				Code.put(Code.bastore);
			}
		} else {
			if (o.getType() == MyTab.intType)
				Code.put(Code.read);
			else 
				Code.put(Code.bread);
			Code.store(o);
		}
	}

	public void visit(FindAnyStatement findAnyStatement) {
		int storeSize;
		if (findAnyBoolVarDesignatorObj.getLevel()==0) { // global variable 
            storeSize = 3;
        } else {
            // local variable 
            if (0 <= findAnyBoolVarDesignatorObj.getAdr() && findAnyBoolVarDesignatorObj.getAdr() <= 3) 
                storeSize = 1;
            else { 
            	  storeSize = 2;
            } 
        }
		
		storeExpr(findAnyStatement.getExpr().struct);
		Code.loadConst(-1);
		Code.put(Code.add);
		Code.put(Code.dup2);
		Code.put(Code.dup);
		Code.loadConst(-1);
		Code.put(Code.jcc + Code.eq); Code.put2(16 + storeSize + 3); // skok na kraj uz dodjelu false
		if (findAnyArrayDesignatorObj.getType().getElemType() == MyTab.intType)
			Code.put(Code.aload);
		else
			Code.put(Code.baload);
		loadExpr(findAnyStatement.getExpr().struct);
		Code.put(Code.jcc + Code.eq); Code.put2(5 + 3); // skok na kraj uz dodjelu true
		Code.put(Code.jmp); Code.put2(-18 + 3); // skok na pocetak
		
		Code.put(Code.pop);
		Code.put(Code.pop);
		
		// Dodjela true
		Code.loadConst(1);
		Code.store(findAnyBoolVarDesignatorObj);
		Code.put(Code.jmp); Code.put2(2 + 3);
		
		// Dodjela false
		Code.loadConst(0);
		Code.store(findAnyBoolVarDesignatorObj);
	}
	
	public void visit(AddExpr addExpr) {
		if (addExpr.getAddop() instanceof PlusOperator)
			Code.put(Code.add);
		else 
			Code.put(Code.sub);
	}
	
	public void visit(TermExprMinus termExprMinus) {
		Code.put(Code.neg);
	}
	
	public void visit(MulTerm mulTerm) {
		if (mulTerm.getMulop() instanceof MulOperator)
			Code.put(Code.mul);
		else if (mulTerm.getMulop() instanceof DivOperator)
			Code.put(Code.div);
		else 
			Code.put(Code.rem);
	}
	
	public void visit(FactorConst factorConst) {
		Obj constNode = new Obj(Obj.Con, "$", factorConst.struct);
		constNode.setLevel(0);
		if (factorConst.struct == MyTab.intType)
    		constNode.setAdr(numberConstValue);
    	else if (factorConst.struct == MyTab.charType)
    		constNode.setAdr(charConstValue);
		else
			constNode.setAdr(boolConstValue ? 1 : 0);
		Code.load(constNode);
	}
	
	public void visit(ConstNumber cnst){
		cnst.struct = MyTab.intType;
		numberConstValue = cnst.getN1();
	}
	
	public void visit(ConstBoolean cnst){
		cnst.struct = MyTab.boolType;
		boolConstValue = cnst.getB1();
	}
	
	public void visit(ConstChar cnst){
		cnst.struct = MyTab.charType;
		charConstValue = cnst.getC1();
	}
	
	public void visit(FactorNewArray factorNewArray) {
		Code.put(Code.newarray);
		if (factorNewArray.getType().struct == MyTab.intType)
			Code.put(1);
		else 
			Code.put(0);
	}
	
	public void visit(DesignatorVar designatorVar) {
		SyntaxNode parent = designatorVar.getParent();
//		designatorObj = MyTab.find(designatorVar.getVar());
//		designatorVar.obj = designatorObj;
		designatorObj = designatorVar.obj;
		if (AssignStatement.class == parent.getClass() /*&& FunctionCall.class != parent.getClass()*/) {
			storingArrayElement = false;
		} else if (FindAnyStatement.class == parent.getClass()) {
			if (findAnyDesignatorCounter == 0) {
				findAnyBoolVarDesignatorObj = designatorObj;
			} else {
				findAnyArrayDesignatorObj = designatorObj;
				Code.load(findAnyArrayDesignatorObj);
				Code.load(findAnyArrayDesignatorObj);
				Code.put(Code.arraylength);
			}
			findAnyDesignatorCounter = (findAnyDesignatorCounter + 1) % 2;
		} else {
			Code.load(designatorObj);
		}
	}
	public void visit(DesignatorArray designatorArray) {
		SyntaxNode parent = designatorArray.getParent();
//		designatorObj = MyTab.find(designatorArray.getVar());
//		designatorArray.obj = designatorObj;
		designatorObj = designatorArray.obj;
		if (AssignStatement.class == parent.getClass() || parent.getClass() == ReadStatement.class) {
			// na steku je index, trebam biti adr, index
			// stavi adr, pa zamijeni mjesta i popuj
			storingArrayElement = designatorObj.getType().getKind() == Struct.Array;
			
//			if (designatorObj.getLevel()==0) { // global variable 
//	        	Code.put(Code.getstatic); Code.put2(designatorObj.getAdr());
//	        } else {
//	        	// local variable
//		        if (0 <= designatorObj.getAdr() && designatorObj.getAdr() <= 3) 
//		        	Code.put(Code.load_n + designatorObj.getAdr());
//		        else { 
//		        	Code.put(Code.load); Code.put(designatorObj.getAdr()); 
//		        } 
//	        }
			Code.load(designatorObj);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
		} else {
			// na steku je index, trebam biti adr, index
			// stavi adr, pa zamijeni mjesta i popuj
			
//			if (designatorObj.getLevel()==0) { // global variable 
//	        	Code.put(Code.getstatic); Code.put2(designatorObj.getAdr());
//	        } else {
//	        	// local variable
//		        if (0 <= designatorObj.getAdr() && designatorObj.getAdr() <= 3) 
//		        	Code.put(Code.load_n + designatorObj.getAdr());
//		        else { 
//		        	Code.put(Code.load); Code.put(designatorObj.getAdr()); 
//		        } 
//	        }
			Code.load(designatorObj);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			if (parent.getClass() == IncrementStatement.class || parent.getClass() == DecrementStatement.class)
				Code.put(Code.dup2);
			if (designatorObj.getType().getElemType() == MyTab.intType)
				Code.put(Code.aload);
			else
				Code.put(Code.baload);
		}
	}
	
	public void visit(Type type) {
    	Obj typeNode = MyTab.find(type.getTypeName());
    	if (typeNode == MyTab.noObj) {
    		type.struct = MyTab.noType;
    	} else {
    		if (Obj.Type == typeNode.getKind()) {
    			type.struct = typeNode.getType();
    		} else {
    			type.struct = MyTab.noType;
    		}
    	}
    }
}
