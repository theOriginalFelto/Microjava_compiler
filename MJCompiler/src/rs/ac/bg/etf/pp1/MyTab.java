package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class MyTab extends Tab {
	public static final Struct boolType = new Struct(Struct.Bool), 
			intArray = new Struct(Struct.Array, MyTab.intType),
			charArray = new Struct(Struct.Array, MyTab.charType),
			boolArray = new Struct(Struct.Array, MyTab.boolType);
	
	public static void init() {
		Tab.init();
		
		currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
	}
}
