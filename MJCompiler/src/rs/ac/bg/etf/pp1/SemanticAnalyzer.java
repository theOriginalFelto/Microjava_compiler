package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {
	int printCallCount = 0;
	int varDeclarationCount = 0;
	boolean returnFound = false, errorDetected = false;
	int numberOfVars;
	
	Obj currentMethod = null;
	
	Logger log = Logger.getLogger(getClass());
	
	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message); 
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.info(msg.toString());
	}

	public void visit(VarDeclaration varDeclaration){
		varDeclarationCount++;
		report_info("Desklarisana promjenljiva " + varDeclaration.getVarName(), varDeclaration);
		Obj varNode = Tab.insert(Obj.Var, varDeclaration.getVarName(), varDeclaration.getType().struct);
	}
	
    public void visit(PrintStatement print) {
		printCallCount++;
	}
    
    public void visit(Program program) {
    	numberOfVars = Tab.currentScope.getnVars();
    	Tab.chainLocalSymbols(program.getProgName().obj);
    	Tab.closeScope();
    }
    public void visit(ProgName progName) {
    	progName.obj = Tab.insert(Obj.Prog, progName.getProgramName(), Tab.noType);
    	Tab.openScope();
    }
    
    public void visit(Type type) {
    	Obj typeNode = Tab.find(type.getTypeName());
    	if (typeNode == Tab.noObj) {
    		report_error("Nije pronadjen tip " + type.getTypeName() + " u tabeli simbola! ", null);
    		type.struct = Tab.noType;
    	} else {
    		if (Obj.Type == typeNode.getKind()) {
    			type.struct = typeNode.getType();
    		} else {
    			report_error("Greska: Ime " + type.getTypeName() + " ne predstavlja tip!", type);
    			type.struct = Tab.noType;
    		}
    	}
    }
    
    public void visit(MethodVoidTypeDecl methodVoidTypeDecl) {
    	currentMethod = Tab.insert(Obj.Meth, methodVoidTypeDecl.getMethName(), Tab.noType /* TODO Void tip*/);
    	methodVoidTypeDecl.obj = currentMethod;
    	Tab.openScope();
    	report_info("Obradjuje se funkcija " + methodVoidTypeDecl.getMethName(), methodVoidTypeDecl);
    }
    
    public void visit(MethodTypeDecl methodTypeDecl) {
    	currentMethod = Tab.insert(Obj.Meth, methodTypeDecl.getMethName(), methodTypeDecl.getType().struct);
    	methodTypeDecl.obj = currentMethod;
    	Tab.openScope();
		report_info("Obradjuje se funkcija " + methodTypeDecl.getMethName(), methodTypeDecl);
    }
    
    public void visit(MethodDeclaration methodDeclaration) {
    	if (!returnFound && currentMethod.getType() != Tab.noType) {
			report_error("Semanticka greska na liniji " + methodDeclaration.getLine() + 
					": funkcija " + currentMethod.getName() + " nema return iskaz!", null);
		}
    	Tab.chainLocalSymbols(currentMethod);
    	Tab.closeScope();
    	
    	returnFound = false;    	
    	currentMethod = null;
    }
    
    public void visit(DesignatorVar designatorVar) {
    	Obj obj = Tab.find(designatorVar.getVar());
    	if (obj == Tab.noObj) { 
			report_error("Greska na liniji " + designatorVar.getLine()+ " : ime " + designatorVar.getVar()+" nije deklarisano! ", null);
		}
    	designatorVar.obj = obj;
    }
    
    public void visit(AddExpr addExpr) {
		Struct te = addExpr.getExpr().struct;
		Struct t = addExpr.getTerm().struct;
		if (te.equals(t) && te == Tab.intType)
			addExpr.struct = te;
		else {
			report_error("Greska na liniji "+ addExpr.getLine()+" : nekompatibilni tipovi u izrazu za sabiranje.", null);
			addExpr.struct = Tab.noType;
		} 
	}

	public void visit(TermExpr termExpr) {
		termExpr.struct = termExpr.getTerm().struct;
	}
	
	public void visit(TermExprMinus termExpr) {
		termExpr.struct = termExpr.getTerm().struct;
	}

	public void visit(MulTerm mulTerm) {
		Struct te = mulTerm.getFactor().struct;
		Struct t = mulTerm.getTerm().struct;
		if (te.equals(t) && te == Tab.intType)
			mulTerm.struct = te;
		else {
			report_error("Greska na liniji "+ mulTerm.getLine()+" : nekompatibilni tipovi u izrazu za mnozenje/dijeljenje.", null);
			mulTerm.struct = Tab.noType;
		}    	
	}
	
	public void visit(FactorTerm factorTerm) {
		factorTerm.struct = factorTerm.getFactor().struct;
	}
	
	public void visit(FactorConst factorConst) {
		factorConst.struct = factorConst.getConstValue().struct;
	}

	public void visit(ConstNumber cnst){
		cnst.struct = Tab.intType;    	
	}
	
	public void visit(ConstBoolean cnst){
		cnst.struct = Tab.intType;
		// TODO prosiriti tabelu sa bool tipom
	}
	
	public void visit(ConstChar cnst){
		cnst.struct = Tab.charType;    	
	}
	
	public void visit(FactorVar factorVar) {
		factorVar.struct = factorVar.getDesignator().obj.getType();
	}
	
	public void visit(FactorNewArray factorNewArray) {
		factorNewArray.struct = new Struct(Struct.Array, factorNewArray.getType().struct);
	}
	
	public void visit(FactorParenthesisExpression factorParenthesisExpression) {
		factorParenthesisExpression.struct = factorParenthesisExpression.getExpr().struct;
	}
	
	
	public void visit(ReturnExprStatement returnExprStatement) {
		returnFound = true;
		Struct currentMethType = currentMethod.getType();
		if (!currentMethType.compatibleWith(returnExprStatement.getExpr().struct)) {
			report_error("Greska na liniji " + returnExprStatement.getLine() + " : " + 
					"tip izraza u return naredbi ne slaze se sa tipom povratne vrednosti funkcije " + currentMethod.getName(), null);
		}	
	}
	
	
	public void visit(AssignStatement assignStatement) {
		if (!assignStatement.getExpr().struct.assignableTo(assignStatement.getDesignator().obj.getType())) {
			report_error("Greska na liniji " + assignStatement.getLine() + " : " + " nekompatibilni tipovi u dodeli vrednosti ", null);
		}
	}
	
	
	
	
	public boolean passed() { return !errorDetected; }
}
