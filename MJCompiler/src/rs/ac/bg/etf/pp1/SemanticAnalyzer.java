package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {
	int printCallCount = 0, varDeclarationCount = 0;
	int errorsDetected = 0;
	boolean returnFound = false, mainMethodFound = false;
	boolean newArrayCreation = false, arrayUsedInsteadOfVar = false;
	boolean getArrayElemType = false;
	int numberOfVars;
	
	int numberConstValue, boolConstValue;
	char charConstValue;
	
	Obj currentMethod = null;
	Type currentDeclaredType = null;
	String currentConstValueTypeName = null;
	
	Logger log = Logger.getLogger(getClass());

	public boolean passed() { return errorsDetected == 0 && mainMethodFound; }
	
	public void report_error(String message, SyntaxNode info) {
		errorsDetected++;
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
		if (MyTab.find(symbolName).getKind() == Obj.Type) {
			report_error("Greska na liniji " + line + ": ime promjenljive ne smije biti predefinisani tip!", null);
			return true;
		}
		if (MyTab.find(symbolName) == MyTab.noObj)
			return false;
		if (MyTab.currentScope.findSymbol(symbolName) == null)
			return false;
		report_error("Semanticka greska na liniji " + line + ": simbol " + symbolName + 
				" je vec definisan!", null);
		return true;
	}
	
	public boolean isMainMethod() {
		if (currentMethod.getType() == MyTab.noType && currentMethod.getName().equals("main")) 
			return true;
		return false;
	}
    
    public void visit(Program program) {
    	numberOfVars = MyTab.currentScope.getnVars();
    	MyTab.chainLocalSymbols(program.getProgName().obj);
    	MyTab.closeScope();
    }
    public void visit(ProgName progName) {
    	progName.obj = MyTab.insert(Obj.Prog, progName.getProgramName(), MyTab.noType);
    	MyTab.openScope();
    	MyTab.insert(Obj.Var, "findAnyIntExpr", MyTab.intType);
    	MyTab.insert(Obj.Var, "findAnyCharExpr", MyTab.charType);
    	MyTab.insert(Obj.Var, "findAnyBoolExpr", MyTab.boolType);
    }
    
    public void visit(MultipleVarDecl multipleVarDecl) {
    	multipleVarDecl.struct = multipleVarDecl.getVarDecl().struct;
    }
    
    public void visit(MultipleArrayDecl multipleArrayDecl) {
    	multipleArrayDecl.struct = multipleArrayDecl.getVarDecl().struct;
    }

	public void visit(VarDeclaration varDeclaration) {
		varDeclarationCount++;
		currentDeclaredType = varDeclaration.getType();
		
		if (symbolExistsInCurrentScope(varDeclaration.getVarName(), varDeclaration.getLine()))
			return;
		
		if (varDeclaration.getType().struct == MyTab.noType) {
			report_error("Greska pri deklarisanju promjenljive " + varDeclaration.getVarName() + 
					" na liniji " + varDeclaration.getLine() + ". " + 
					varDeclaration.getType().getTypeName() + " nije podrzan tip!", null);
			return;
		}
		
		report_info("Desklarisana promjenljiva '" + varDeclaration.getVarName() + 
				"' tipa " + varDeclaration.getType().getTypeName(), varDeclaration);
		MyTab.insert(Obj.Var, varDeclaration.getVarName(), varDeclaration.getType().struct);
//		Obj varNode = MyTab.insert(Obj.Var, varDeclaration.getVarName(), varDeclaration.getType().struct);
		varDeclaration.struct = varDeclaration.getType().struct;
	}
	
	public void visit(ArrayDecl arrayDecl) {
		varDeclarationCount++;
		currentDeclaredType = arrayDecl.getType();
		
		if (symbolExistsInCurrentScope(arrayDecl.getVarName(), arrayDecl.getLine()))
			return;
		if (arrayDecl.getType().struct == MyTab.noType) {
			report_error("Greska pri deklarisanju niza " + arrayDecl.getVarName() + 
					" na liniji " + arrayDecl.getLine() + ". " + 
					arrayDecl.getType().getTypeName() + " nije podrzan tip!", null);
			return;
		}
		
		arrayDecl.struct = new Struct(Struct.Array, arrayDecl.getType().struct);
		report_info("Desklarisan niz '" + arrayDecl.getVarName() + 
				"' tipa " + arrayDecl.getType().getTypeName(), arrayDecl);
		MyTab.insert(Obj.Var, arrayDecl.getVarName(), arrayDecl.struct);
//		Obj arrNode = MyTab.insert(Obj.Var, arrayDecl.getVarName(), arrayDecl.struct);
	}
    
    public void visit(MultVarDecl multVarDecl) {
		varDeclarationCount++;
    	if (symbolExistsInCurrentScope(multVarDecl.getVarName(), multVarDecl.getLine()))
			return;
    	if (currentDeclaredType.struct == MyTab.noType) {
			report_error("Greska pri deklarisanju promjenljive " + multVarDecl.getVarName() + 
					" na liniji " + multVarDecl.getLine() + ". " + 
					currentDeclaredType.getTypeName() + " nije podrzan tip!", null);
			return;
		}
		
		report_info("Desklarisana promjenljiva '" + multVarDecl.getVarName() + 
				"' tipa " + currentDeclaredType.getTypeName(), multVarDecl);
		MyTab.insert(Obj.Var, multVarDecl.getVarName(), currentDeclaredType.struct);
//		Obj arrNode = MyTab.insert(Obj.Var, multVarDecl.getVarName(), currentDeclaredType.struct);
    }
    
    public void visit(MultArrDecl multArrDecl) {
		varDeclarationCount++;
    	if (symbolExistsInCurrentScope(multArrDecl.getVarName(), multArrDecl.getLine()))
    		return;
    	if (currentDeclaredType.struct == MyTab.noType) {
			report_error("Greska pri deklarisanju niza " + multArrDecl.getVarName() + 
					" na liniji " + multArrDecl.getLine() + ". " + 
					currentDeclaredType.getTypeName() + " nije podrzan tip!", null);
			return;
		}
    	
    	multArrDecl.struct = new Struct(Struct.Array, currentDeclaredType.struct);
    	report_info("Desklarisan niz '" + multArrDecl.getVarName() + 
    			"' tipa " + currentDeclaredType.getTypeName(), multArrDecl);
    	MyTab.insert(Obj.Var, multArrDecl.getVarName(), multArrDecl.struct);
//    	Obj arrNode = MyTab.insert(Obj.Var, multArrDecl.getVarName(), multArrDecl.struct);
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
    	
    	if (!currentConstValueTypeName.equals(currentDeclaredType.getTypeName())) {
    		report_error("Semanticka greska na liniji " + constDeclaration.getLine() + 
    				": literal se ne poklapa sa tipom konstante " + constDeclaration.getConstVarName(), null);
    		return;
    	}
    	
    	report_info("Desklarisana konstanta '" + constDeclaration.getConstVarName() + 
    			"' tipa " + currentDeclaredType.getTypeName(), constDeclaration);
    	Obj constNode = MyTab.insert(Obj.Con, constDeclaration.getConstVarName(), currentDeclaredType.struct);
    	if (currentConstValueTypeName == "int")
    		constNode.setAdr(numberConstValue);
    	else if (currentConstValueTypeName == "char")
    		constNode.setAdr(charConstValue);
    	else
    		constNode.setAdr(boolConstValue);
    	constNode.setLevel(0);
    }
    
    public void visit(ConstDeclaration constDeclaration) {
    	currentDeclaredType = constDeclaration.getType();
    	if (symbolExistsInCurrentScope(constDeclaration.getConstVarName(), constDeclaration.getLine()))
			return;
    	
    	if (!currentConstValueTypeName.equals(currentDeclaredType.getTypeName())) {
    		report_error("Semanticka greska na liniji " + constDeclaration.getLine() + 
    				": literal se ne poklapa sa tipom konstante " + constDeclaration.getConstVarName(), null);
    		return;
    	}
		
		report_info("Desklarisana konstanta '" + constDeclaration.getConstVarName() + 
				"' tipa " + currentDeclaredType.getTypeName(), constDeclaration);
		Obj constNode = MyTab.insert(Obj.Con, constDeclaration.getConstVarName(), currentDeclaredType.struct);
		if (currentConstValueTypeName == "int")
    		constNode.setAdr(numberConstValue);
    	else if (currentConstValueTypeName == "char")
    		constNode.setAdr(charConstValue);
    	else
    		constNode.setAdr(boolConstValue);
    	constNode.setLevel(0);
    }
    
    public void visit(MethodVoidTypeDecl methodVoidTypeDecl) {
    	if (MyTab.find(methodVoidTypeDecl.getMethName()) != MyTab.noObj) {
    		report_error("Greska na liniji " + methodVoidTypeDecl.getLine() + ": metoda " + 
    				methodVoidTypeDecl.getMethName() + " ne moze biti deklarisana jer to ime vec postoji", null);
    		return;
    	}
    		
    	currentMethod = MyTab.insert(Obj.Meth, methodVoidTypeDecl.getMethName(), MyTab.noType);
    	methodVoidTypeDecl.obj = currentMethod;
    	MyTab.openScope();
    	report_info("Obradjuje se funkcija " + methodVoidTypeDecl.getMethName(), methodVoidTypeDecl);
    }
    
    public void visit(MethodTypeDecl methodTypeDecl) {
    	if (MyTab.find(methodTypeDecl.getMethName()) != MyTab.noObj) {
    		report_error("Greska na liniji " + methodTypeDecl.getLine() + ": metoda " + 
    				methodTypeDecl.getMethName() + " ne moze biti deklarisana jer to ime vec postoji", null);
    		return;
    	}
    		
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
    	mainMethodFound = mainMethodFound || isMainMethod();
    	MyTab.chainLocalSymbols(currentMethod);
    	MyTab.closeScope();
    	
    	returnFound = false;    	
    	currentMethod = null;
    }
    
    public void visit(FormalVarDecl formalVarDecl) {
    	if (isMainMethod()) {
    		report_error("Greska na liniji " + formalVarDecl.getLine() + ": main metoda ne sme imati formalne parametre!", null);
    		return;
    	}
    	if (symbolExistsInCurrentScope(formalVarDecl.getVarName(), formalVarDecl.getLine()))
			return;
    	if (formalVarDecl.getType().struct == MyTab.noType) {
			report_error("Greska pri deklarisanju formalnog parametra " + formalVarDecl.getVarName() + 
					" na liniji " + formalVarDecl.getLine() + ". " + 
					formalVarDecl.getType().getTypeName() + " nije podrzan tip!", null);
			return;
		}
		
		report_info("Desklarisan formalni parametar '" + formalVarDecl.getVarName() + 
				"' tipa " + formalVarDecl.getType().getTypeName() + " za metodu " + currentMethod.getName(), formalVarDecl);
		MyTab.insert(Obj.Var, formalVarDecl.getVarName(), formalVarDecl.getType().struct);
//		Obj varNode = MyTab.insert(Obj.Var, formalVarDecl.getVarName(), formalVarDecl.getType().struct);
    }
    
    public void visit(FormalArrDecl formalArrDecl) {
    	if (isMainMethod()) {
    		report_error("Greska na liniji " + formalArrDecl.getLine() + ": main metoda ne sme imati formalne parametre!", null);
    		return;
    	}
    	if (symbolExistsInCurrentScope(formalArrDecl.getVarName(), formalArrDecl.getLine()))
			return;
    	if (formalArrDecl.getType().struct == MyTab.noType) {
			report_error("Greska pri deklarisanju formalnog parametra " + formalArrDecl.getVarName() + 
					" na liniji " + formalArrDecl.getLine() + ". " + 
					formalArrDecl.getType().getTypeName() + " nije podrzan tip!", null);
			return;
		}
    	
		report_info("Desklarisan formalni parametar '" + formalArrDecl.getVarName() + 
				"' tipa niza " + formalArrDecl.getType().getTypeName() + " za metodu " + currentMethod.getName(), formalArrDecl);
		MyTab.insert(Obj.Var, formalArrDecl.getVarName(), new Struct(Struct.Array, formalArrDecl.getType().struct));
//		Obj arrNode = MyTab.insert(Obj.Var, formalArrDecl.getVarName(), new Struct(Struct.Array, formalArrDecl.getType().struct));
    }

	public void visit(ReturnExprStatement returnExprStatement) {
		returnFound = true;
		Struct currentMethType = currentMethod.getType();
		if (!currentMethType.compatibleWith(returnExprStatement.getExpr().struct)) {
			report_error("Greska na liniji " + returnExprStatement.getLine() + ": " + 
					"tip izraza u return naredbi ne slaze se sa tipom povratne vrednosti funkcije " + currentMethod.getName(), null);
		}	
	}
	
	public void visit(ReturnNoExprStatement returnNoExprStatement) {
		returnFound = true;
		Struct currentMethType = currentMethod.getType();
		if (!currentMethType.equals(MyTab.noType)) {
			report_error("Greska na liniji " + returnNoExprStatement.getLine() + ": u metodi " + 
					currentMethod.getName() + " ne smije biti return iskaz bez izraza tipa " + currentMethod.getType(), null);
		}	
	}
	
	
    
    public void visit(AddExpr addExpr) {
		Struct te = addExpr.getExpr().struct;
		Struct t = addExpr.getTerm().struct;
		if (te.equals(t) && te == MyTab.intType)
			addExpr.struct = te;
		else {
			report_error("Greska na liniji " + addExpr.getLine() + ": nekompatibilni tipovi u izrazu za sabiranje.", null);
			addExpr.struct = MyTab.noType;
		} 
	}

	public void visit(TermExpr termExpr) {
		termExpr.struct = termExpr.getTerm().struct;
	}
	
	public void visit(TermExprMinus termExpr) {
		termExpr.struct = termExpr.getTerm().struct;
		if (termExpr.getTerm().struct != MyTab.intType)
			report_error("Greska: negira se stavka koja nije tipa int", termExpr);
	}

	public void visit(MulTerm mulTerm) {
		Struct te = mulTerm.getFactor().struct;
		Struct t = mulTerm.getTerm().struct;
		if (te.equals(t) && te == MyTab.intType)
			mulTerm.struct = te;
		else {
			report_error("Greska na liniji " + mulTerm.getLine() + ": nekompatibilni tipovi u izrazu za mnozenje/dijeljenje.", null);
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
	
	public void visit(FactorVar factorVar) {
		Struct designatorType = factorVar.getDesignator().obj.getType();
		if (getArrayElemType)
			designatorType = designatorType.getElemType();
			
		factorVar.struct = designatorType;
	}
	
	public void visit(FactorNewArray factorNewArray) {
		factorNewArray.struct = new Struct(Struct.Array, factorNewArray.getType().struct);
		if (factorNewArray.getExpr().struct != MyTab.intType)
			report_error("Greska: izraz za duzinu niza pri kreiranju mora biti tipa int", factorNewArray);
		newArrayCreation = true;
	}
	
	public void visit(FactorParenthesisExpression factorParenthesisExpression) {
		factorParenthesisExpression.struct = factorParenthesisExpression.getExpr().struct;
	}
	
	
	
	public void visit(AssignStatement assignStatement) {
		Struct designatorType = assignStatement.getDesignator().obj.getType();
		if (assignStatement.getDesignator().obj.getKind() == Obj.Con) {
			report_error("Semanticka greska: nedozvoljena dodjela vrijenosti konstanti", assignStatement);
			return;
		}
		
		if (newArrayCreation) {
			if (!assignStatement.getExpr().struct.assignableTo(designatorType)) {
				report_error("Greska: nekompatibilni tipovi pri dodjeli vrijednosti", assignStatement);
			}
		} else {
			if (!arrayUsedInsteadOfVar && designatorType.getElemType() != null)
				designatorType = designatorType.getElemType();
			if (arrayUsedInsteadOfVar)
				report_error("Greska: nedozvoljena dodjela vrijednosti nizu " + 
						assignStatement.getDesignator().obj.getName(), assignStatement);
			if (!arrayUsedInsteadOfVar && !assignStatement.getExpr().struct.assignableTo(designatorType)) {
				report_error("Greska: nekompatibilni tipovi pri dodjeli vrijednosti", assignStatement);
			}
		}
		newArrayCreation = false;
	}
	
	public void visit(IncrementStatement incrementStatement) {
		Struct designatorType = incrementStatement.getDesignator().obj.getType();
		if (incrementStatement.getDesignator().obj.getKind() == Obj.Con) {
			report_error("Semanticka greska: nedozvoljeno inkrementiranje konstante", incrementStatement);
			return;
		}
		
		if (!arrayUsedInsteadOfVar && designatorType.getElemType() != null)
			designatorType = designatorType.getElemType();
		if (arrayUsedInsteadOfVar)
			report_error("Greska: nedozvoljeno inkrementiranje niza " + 
					incrementStatement.getDesignator().obj.getName(), incrementStatement);
		if (!arrayUsedInsteadOfVar && designatorType != MyTab.intType) 
			report_error("Greska: ime " + incrementStatement.getDesignator().obj.getName() + 
					" nije tipa int pri operatoru ++", incrementStatement);
	}
	
	public void visit(DecrementStatement decrementStatement) {
		Struct designatorType = decrementStatement.getDesignator().obj.getType();
		if (decrementStatement.getDesignator().obj.getKind() == Obj.Con) {
			report_error("Semanticka greska: nedozvoljeno dekrementiranje konstante", decrementStatement);
			return;
		}
		
		if (!arrayUsedInsteadOfVar && designatorType.getElemType() != null)
			designatorType = designatorType.getElemType();
		if (arrayUsedInsteadOfVar)
			report_error("Greska: nedozvoljeno dekrementiranje niza " + 
					decrementStatement.getDesignator().obj.getName(), decrementStatement);
		if (!arrayUsedInsteadOfVar && designatorType != MyTab.intType)  
			report_error("Greska: ime " + decrementStatement.getDesignator().obj.getName() + 
					" nije tipa int pri operatoru --", decrementStatement);
	}
	
	public void visit(ReadStatement readStatement) {
		Struct designatorType = readStatement.getDesignator().obj.getType();
		if (readStatement.getDesignator().obj.getKind() == Obj.Con) {
			report_error("Semanticka greska: nedozvoljeno ucitavanje vrijenosti u konstantu", readStatement);
			return;
		}
		
		if (!arrayUsedInsteadOfVar && designatorType.getElemType() != null)
			designatorType = designatorType.getElemType();
		if (arrayUsedInsteadOfVar)
			report_error("Greska: nedozvoljena upotreba niza " + 
					readStatement.getDesignator().obj.getName() + " pri read iskazu", readStatement);
		if (!arrayUsedInsteadOfVar && designatorType != MyTab.intType && 
				designatorType != MyTab.boolType && designatorType != MyTab.charType)
			report_error("Greska: ime " + readStatement.getDesignator().obj.getName() + 
					" nije dozvoljenog tipa pri read iskazu", readStatement);
	}
	
    public void visit(PrintStatement printStatement) {
		printCallCount++;
		int typeKind = printStatement.getExpr().struct.getKind();
		if (typeKind != Struct.Int && typeKind != Struct.Bool && typeKind != Struct.Char)
			report_error("Greska: izraz nije dozvoljenog tipa pri print iskazu", printStatement);
	}
    
    public void visit(PrintStatementNumber printStatementNumber) {
    	printCallCount++;
		int typeKind = printStatementNumber.getExpr().struct.getKind();
		if (typeKind != Struct.Int && typeKind != Struct.Bool && typeKind != Struct.Char)
			report_error("Greska: izraz nije dozvoljenog tipa pri print iskazu", printStatementNumber);
    }
    
    public void visit(FindAnyStatement findAnyStatement) {
		if (findAnyStatement.getDesignator().obj.getKind() == Obj.Con) {
			report_error("Semanticka greska: nedozvoljena dodjela vrijenosti konstanti", findAnyStatement);
			return;
		}
		
    	if (findAnyStatement.getDesignator().obj.getType() != MyTab.boolType)
    		report_error("Greska kod findAny iskaza: ime sa lijeve strane znaka = nije tipa bool", findAnyStatement);
    	
    	int kind = findAnyStatement.getDesignator1().obj.getType().getKind();
    	Struct elemType = findAnyStatement.getDesignator1().obj.getType().getElemType();
    	if (kind != Struct.Array && elemType != MyTab.intType && elemType != MyTab.boolType && elemType != MyTab.charType)
    		report_error("Greska kod findAny iskaza: ime sa desne strane znaka = nije niz ugradjenog tipa", findAnyStatement);
    	
    	if (!elemType.equals(findAnyStatement.getExpr().struct))
    		report_error("Greska kod findAny iskaza: tip izraza koji se trazi se ne poklapa sa tipom elemenata niza", findAnyStatement);
    }
	
	public void visit(DesignatorVar designatorVar) {
    	Obj obj = MyTab.find(designatorVar.getVar());
    	if (obj == MyTab.noObj)
			report_error("Greska na liniji " + designatorVar.getLine()+ ": ime " + designatorVar.getVar() + " nije deklarisano! ", null);
    	if (obj.getType().getKind() == Struct.Array) {
//    		report_error("Greska: nedozvoljeno koriscenje niza " + obj.getName(), designatorVar);
    		arrayUsedInsteadOfVar = true;
    	}
    	else
    		arrayUsedInsteadOfVar = false;
    	
    	designatorVar.obj = obj;
    	getArrayElemType = false;
    }
	
	public void visit(DesignatorArray designatorArray) {
		Obj obj = MyTab.find(designatorArray.getVar());
		if (obj == MyTab.noObj)
			report_error("Greska na liniji " + designatorArray.getLine()+ ": ime " + designatorArray.getVar() + " nije deklarisano! ", null);
		
		if (obj.getType().getKind() != Struct.Array)
			report_error("Greska: nedozvoljeno indeksiranje promjenljive umjesto niza", designatorArray);
			
		if (designatorArray.getExpr().struct != MyTab.intType)
			report_error("Greska: izraz za indeksiranje niza '" + designatorArray.getVar() + "' nije tipa int", designatorArray);
		
		designatorArray.obj = obj;
		arrayUsedInsteadOfVar = false;
		getArrayElemType = true;
	}
}
