package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {
	int printCallCount = 0;
	int varDeclarationCount = 0;
	boolean returnFound = false, errorDetected = false;
	int numberOfVars;
	
	int numberConstValue, boolConstValue;
	char charConstValue;
	
	Obj currentMethod = null;
	Type currentDeclaredType = null;
	String currentDeclaredConstTypeName = null;
	
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
	
	public boolean symbolExistsInCurrentScope(String symbolName, int line) {
		if (MyTab.find(symbolName) == MyTab.noObj)
			return false;
		if (MyTab.currentScope.findSymbol(symbolName) == null)
			return false;
		report_error("Semanticka greska na liniji " + line + ": simbol " + symbolName + 
				" je vec definisan!", null);
		return false;
	}
	
    public void visit(PrintStatement print) {
		printCallCount++;
	}
    
    public void visit(Program program) {
    	numberOfVars = MyTab.currentScope.getnVars();
    	MyTab.chainLocalSymbols(program.getProgName().obj);
    	MyTab.closeScope();
    }
    public void visit(ProgName progName) {
    	progName.obj = MyTab.insert(Obj.Prog, progName.getProgramName(), MyTab.noType);
    	MyTab.openScope();
    }
    
    public void visit(MultipleVarDecl multipleVarDecl) {
    	multipleVarDecl.struct = multipleVarDecl.getVarDecl().struct;
    }
    
    public void visit(MultipleArrayDecl multipleArrayDecl) {
    	multipleArrayDecl.struct = multipleArrayDecl.getVarDecl().struct;
    }

	public void visit(VarDeclaration varDeclaration){
		varDeclarationCount++;
		
		if (symbolExistsInCurrentScope(varDeclaration.getVarName(), varDeclaration.getLine()))
			return;
		
		//TODO uradi provjeru da li je tip NoType za svaku deklaraciju promjenljive
		
		report_info("Desklarisana promjenljiva '" + varDeclaration.getVarName() + 
				"' tipa " + varDeclaration.getType().getTypeName(), varDeclaration);
		Obj varNode = MyTab.insert(Obj.Var, varDeclaration.getVarName(), varDeclaration.getType().struct);
		// TODO vidi ovo jos
		varDeclaration.struct = varDeclaration.getType().struct;
		currentDeclaredType = varDeclaration.getType();
	}
	
	public void visit(ArrayDecl arrayDecl) {
		if (symbolExistsInCurrentScope(arrayDecl.getVarName(), arrayDecl.getLine()))
			return;
		
		arrayDecl.struct = new Struct(Struct.Array, arrayDecl.getType().struct);
		report_info("Desklarisan niz '" + arrayDecl.getVarName() + 
				"' tipa " + arrayDecl.getType().getTypeName(), arrayDecl);
		Obj arrNode = MyTab.insert(Obj.Var, arrayDecl.getVarName(), arrayDecl.struct);
		// TODO vidi ovo jos
		currentDeclaredType = arrayDecl.getType();
	}
    
    public void visit(MultVarDecl multVarDecl) {
    	if (symbolExistsInCurrentScope(multVarDecl.getVarName(), multVarDecl.getLine()))
			return;
		
		report_info("Desklarisana promjenljiva '" + multVarDecl.getVarName() + 
				"' tipa " + currentDeclaredType.getTypeName(), multVarDecl);
		Obj arrNode = MyTab.insert(Obj.Var, multVarDecl.getVarName(), currentDeclaredType.struct);
    }
    
    public void visit(MultArrDecl multArrDecl) {
    	if (symbolExistsInCurrentScope(multArrDecl.getVarName(), multArrDecl.getLine()))
    		return;
    	
    	multArrDecl.struct = new Struct(Struct.Array, currentDeclaredType.struct);
    	report_info("Desklarisan niz '" + multArrDecl.getVarName() + 
    			"' tipa " + currentDeclaredType.getTypeName(), multArrDecl);
    	Obj arrNode = MyTab.insert(Obj.Var, multArrDecl.getVarName(), multArrDecl.struct);
    }
    
    public void visit(Type type) {
    	Obj typeNode = MyTab.find(type.getTypeName());
    	if (typeNode == MyTab.noObj) {
    		report_error("Nije pronadjen tip " + type.getTypeName() + " u tabeli simbola! ", null);
    		type.struct = MyTab.noType;
    	} else {
    		if (Obj.Type == typeNode.getKind()) {
    			type.struct = typeNode.getType();
    		} else {
    			report_error("Greska: Ime " + type.getTypeName() + " ne predstavlja tip!", type);
    			type.struct = MyTab.noType;
    		}
    	}
    }
    
    public void visit(MultipleConstDeclarations constDeclaration) {
    	if (symbolExistsInCurrentScope(constDeclaration.getConstVarName(), constDeclaration.getLine()))
    		return;
    	
    	if (!currentDeclaredConstTypeName.equals(currentDeclaredType.getTypeName())) {
    		report_error("Semanticka greska na liniji " + constDeclaration.getLine() + 
    				": tip vrijednosti konstante se ne poklapa sa deklarisanim tipom!", null);
    		return;
    	}
    	
    	report_info("Desklarisana konstanta '" + constDeclaration.getConstVarName() + 
    			"' tipa " + currentDeclaredType.getTypeName(), constDeclaration);
    	Obj constNode = MyTab.insert(Obj.Var, constDeclaration.getConstVarName(), currentDeclaredType.struct);
    	if (currentDeclaredConstTypeName == "int")
    		constNode.setAdr(numberConstValue);
    	else if (currentDeclaredConstTypeName == "char")
    		constNode.setAdr(charConstValue);
    	else
    		constNode.setAdr(boolConstValue);
    }
    
    public void visit(ConstDeclaration constDeclaration) {
    	if (symbolExistsInCurrentScope(constDeclaration.getConstVarName(), constDeclaration.getLine()))
			return;
    	
    	if (!currentDeclaredConstTypeName.equals(constDeclaration.getType().getTypeName())) {
    		report_error("Semanticka greska na liniji " + constDeclaration.getLine() + 
    				": tip vrijednosti konstante se ne poklapa sa deklarisanim tipom!", null);
    		return;
    	}
		
		report_info("Desklarisana konstanta '" + constDeclaration.getConstVarName() + 
				"' tipa " + constDeclaration.getType().getTypeName(), constDeclaration);
		Obj constNode = MyTab.insert(Obj.Var, constDeclaration.getConstVarName(), constDeclaration.getType().struct);
		if (currentDeclaredConstTypeName == "int")
    		constNode.setAdr(numberConstValue);
    	else if (currentDeclaredConstTypeName == "char")
    		constNode.setAdr(charConstValue);
    	else
    		constNode.setAdr(boolConstValue);
		currentDeclaredType = constDeclaration.getType();
    }
    
    public void visit(MethodVoidTypeDecl methodVoidTypeDecl) {
    	currentMethod = MyTab.insert(Obj.Meth, methodVoidTypeDecl.getMethName(), MyTab.noType);
    	methodVoidTypeDecl.obj = currentMethod;
    	MyTab.openScope();
    	report_info("Obradjuje se funkcija " + methodVoidTypeDecl.getMethName(), methodVoidTypeDecl);
    }
    
    public void visit(MethodTypeDecl methodTypeDecl) {
    	currentMethod = MyTab.insert(Obj.Meth, methodTypeDecl.getMethName(), methodTypeDecl.getType().struct);
    	methodTypeDecl.obj = currentMethod;
    	MyTab.openScope();
		report_info("Obradjuje se funkcija " + methodTypeDecl.getMethName(), methodTypeDecl);
    }
    
    public void visit(MethodDeclaration methodDeclaration) {
    	if (!returnFound && currentMethod.getType() != MyTab.noType) {
			report_error("Semanticka greska na liniji " + methodDeclaration.getLine() + 
					": funkcija " + currentMethod.getName() + " nema return iskaz!", null);
		}
    	MyTab.chainLocalSymbols(currentMethod);
    	MyTab.closeScope();
    	
    	returnFound = false;    	
    	currentMethod = null;
    }
    
    public void visit(DesignatorVar designatorVar) {
    	Obj obj = MyTab.find(designatorVar.getVar());
    	if (obj == MyTab.noObj) { 
			report_error("Greska na liniji " + designatorVar.getLine()+ " : ime " + designatorVar.getVar()+" nije deklarisano! ", null);
		}
    	designatorVar.obj = obj;
    }
    
    public void visit(AddExpr addExpr) {
		Struct te = addExpr.getExpr().struct;
		Struct t = addExpr.getTerm().struct;
		if (te.equals(t) && te == MyTab.intType)
			addExpr.struct = te;
		else {
			report_error("Greska na liniji "+ addExpr.getLine()+" : nekompatibilni tipovi u izrazu za sabiranje.", null);
			addExpr.struct = MyTab.noType;
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
		if (te.equals(t) && te == MyTab.intType)
			mulTerm.struct = te;
		else {
			report_error("Greska na liniji "+ mulTerm.getLine()+" : nekompatibilni tipovi u izrazu za mnozenje/dijeljenje.", null);
			mulTerm.struct = MyTab.noType;
		}    	
	}
	
	public void visit(FactorTerm factorTerm) {
		factorTerm.struct = factorTerm.getFactor().struct;
	}
	
	public void visit(FactorConst factorConst) {
		factorConst.struct = factorConst.getConstValue().struct;
	}

	public void visit(ConstNumber cnst){
		cnst.struct = MyTab.intType;
		numberConstValue = cnst.getN1();
		currentDeclaredConstTypeName = "int";
	}
	
	public void visit(ConstBoolean cnst){
		cnst.struct = MyTab.boolType;
		boolConstValue = cnst.getB1() ? 1 : 0;
		currentDeclaredConstTypeName = "bool";
	}
	
	public void visit(ConstChar cnst){
		cnst.struct = MyTab.charType;
		charConstValue = cnst.getC1();
		currentDeclaredConstTypeName = "char";
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
