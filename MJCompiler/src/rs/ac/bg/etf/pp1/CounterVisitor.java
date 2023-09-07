package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.ArrayDecl;
import rs.ac.bg.etf.pp1.ast.FormalArrDecl;
import rs.ac.bg.etf.pp1.ast.FormalVarDecl;
import rs.ac.bg.etf.pp1.ast.MultArrDecl;
import rs.ac.bg.etf.pp1.ast.MultVarDecl;
import rs.ac.bg.etf.pp1.ast.VarDeclaration;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;

public class CounterVisitor extends VisitorAdaptor {
	protected int count;
	
	public int getCount() { return this.count; }
	
	public static class FormParamCounter extends CounterVisitor {
		public void visit(FormalVarDecl formalVarDecl) {
			count++;
		}
		public void visit(FormalArrDecl formalArrDecl) {
			count++;
		}
	}
	public static class VarCounter extends CounterVisitor {
		public void visit(VarDeclaration varDeclaration) {
			count++;
		}
		public void visit(ArrayDecl arrayDecl) {
			count++;
		}
		public void visit(MultVarDecl multVarDecl) {
			count++;
		}
		public void visit(MultArrDecl multArrDecl) {
			count++;
		}
	}

}
