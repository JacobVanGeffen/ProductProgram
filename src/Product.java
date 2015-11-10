import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import soot.Body;
import soot.Local;
import soot.Modifier;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.AddExpr;
import soot.jimple.AssignStmt;
import soot.jimple.DivExpr;
import soot.jimple.EqExpr;
import soot.jimple.GeExpr;
import soot.jimple.GotoStmt;
import soot.jimple.GtExpr;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.JasminClass;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.LeExpr;
import soot.jimple.LtExpr;
import soot.jimple.MulExpr;
import soot.jimple.NeExpr;
import soot.jimple.RetStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.ThisRef;
import soot.options.Options;
import soot.util.Chain;
import soot.util.JasminOutputStream;

public class Product {

	public static void main(String[] args) throws IOException {
		Product p = new Product("Example2");
		
		SootMethod original = Scene.v().getSootClass("Example2").getMethods().get(2);
		System.out.println(original.retrieveActiveBody());

		SootMethod methodComp = p.selfComp(original);
        SootClass sClass = new SootClass("ProductOutput", Modifier.PUBLIC);
        sClass.addMethod(methodComp);
        
        System.out.println(methodComp.retrieveActiveBody());
        
        writeClass(sClass);
	}
	
	public static void writeClass(SootClass sClass) throws IOException {
        String fileName = SourceLocator.v().getFileNameFor(sClass, Options.output_format_class);
        OutputStream streamOut = new JasminOutputStream(
                                    new FileOutputStream(fileName));
        PrintWriter writerOut = new PrintWriter(
                                    new OutputStreamWriter(streamOut));
        JasminClass jasminClass = new soot.jimple.JasminClass(sClass);
        jasminClass.print(writerOut);
        writerOut.flush();
        streamOut.close();
	}
	
	public Product(String className) {
		Scene.v().loadClassAndSupport(className);
	}

	public Product() {
		Scene.v().loadClassAndSupport("java.lang.Object");
	}
	
	private HashMap<String, Local[]> oldToNewLocals;
	public SootMethod selfComp(SootMethod p) {
		// similar to an import, but also imports dependencies
		Body bOrig = p.retrieveActiveBody();
		SootMethod method = new SootMethod(p.getName()+"xP", p.getParameterTypes(), VoidType.v());
		oldToNewLocals = new HashMap<String, Local[]>();
		
        JimpleBody body = Jimple.v().newBody(method);
        
        method.setActiveBody(body);
            
        // duplicate locals
		for(Local l : bOrig.getLocals()) {
			Local newLocs[] = 
				{ Jimple.v().newLocal("a"+l.getName(), l.getType()), Jimple.v().newLocal("b"+l.getName(), l.getType()) };
			body.getLocals().add(newLocs[0]);
			body.getLocals().add(newLocs[1]);
			oldToNewLocals.put(l.getName(), newLocs);
		}
		
		System.out.println(bOrig.getUnits());
		
		Chain<Unit> units = body.getUnits();
		for(Unit unit : bOrig.getUnits()) {
			if (unit instanceof RetStmt || unit instanceof ReturnStmt)
				continue;
			
			// TODO Keep body?
			Unit[] newUnits = getNewUnits(unit, body);
			
			if (newUnits == null) {
				units.add(unit);
			} else {
				units.addAll(Arrays.asList(newUnits));
			}
		}
		
		return method;
	}
	
	//TODO: Fix loops, they're hard
	private Unit[] getNewUnits(Unit unit, JimpleBody body) {
		if (unit instanceof AssignStmt) {
			
			AssignStmt stmt = (AssignStmt) unit;
			Value[] rightVals = getNewValues(stmt.getRightOp()),
					leftVals = getNewValues(stmt.getLeftOp());
			return new AssignStmt[]{
					Jimple.v().newAssignStmt(leftVals[0], rightVals[0]), 
					Jimple.v().newAssignStmt(leftVals[1], rightVals[1]) 
			};
			
		} else if (unit instanceof IdentityStmt) {
			
			IdentityStmt stmt = (IdentityStmt) unit;
			
			if(stmt.getRightOp() instanceof ThisRef) {
				body.getLocals().add((Local) stmt.getLeftOp());
				return null;
			}
			
			Value[] rightVals = getNewValues(stmt.getRightOp()),
					leftVals = getNewValues(stmt.getLeftOp());
			return new IdentityStmt[]{ 
					Jimple.v().newIdentityStmt(leftVals[0], rightVals[0]), 
					Jimple.v().newIdentityStmt(leftVals[1], rightVals[1]) 
			};
		
		} else if (unit instanceof InvokeStmt) {
			
			InvokeStmt stmt = (InvokeStmt) unit;
			Value[] exprs = getNewValues(stmt.getInvokeExpr());
			return new InvokeStmt[] {
					Jimple.v().newInvokeStmt(exprs[0]),
					Jimple.v().newInvokeStmt(exprs[1])
			};
			
		} else if (unit instanceof IfStmt) {
			
			IfStmt stmt = (IfStmt) unit;
			Unit[] targets = getNewUnits(stmt.getTarget(), body);
			body.getUnits().addAll(Arrays.asList(targets));
			Value[] conditions = getNewValues(stmt.getCondition());
			return new IfStmt[] {
					Jimple.v().newIfStmt(conditions[0], targets[0]),
					Jimple.v().newIfStmt(conditions[1], targets[1])
			};
			
		} else if (unit instanceof GotoStmt) {
			GotoStmt stmt = (GotoStmt) unit;
			Unit[] targets = getNewUnits(stmt.getTarget(), body);
			body.getUnits().addAll(Arrays.asList(targets));
			return new GotoStmt[] {
					Jimple.v().newGotoStmt(targets[0]),
					Jimple.v().newGotoStmt(targets[1])
			};
		}
		System.out.println("Missed: "+unit);
		return new Unit[]{unit, unit};
	}
	
	@SuppressWarnings("unchecked")
	private Value[] getNewValues(Value value) {
		if(oldToNewLocals.containsKey(value.toString())) {
			
			return oldToNewLocals.get(value.toString());
		
		} else if (value instanceof AddExpr) {
		
			AddExpr expr = (AddExpr) value;
			Value[] first = getNewValues(expr.getOp1()),
					second = getNewValues(expr.getOp2());
			return new Value[]{ 
					Jimple.v().newAddExpr(first[0], second[0]), 
					Jimple.v().newAddExpr(first[1], second[1]) 
					};
			
		} else if (value instanceof MulExpr) {
		
			MulExpr expr = (MulExpr) value;
			Value[] first = getNewValues(expr.getOp1()),
					second = getNewValues(expr.getOp2());
			return new Value[]{ 
					Jimple.v().newMulExpr(first[0], second[0]), 
					Jimple.v().newMulExpr(first[1], second[1]) 
					};
			
		} else if (value instanceof DivExpr) {
		
			DivExpr expr = (DivExpr) value;
			Value[] first = getNewValues(expr.getOp1()),
					second = getNewValues(expr.getOp2());
			return new Value[]{ 
					Jimple.v().newDivExpr(first[0], second[0]), 
					Jimple.v().newDivExpr(first[1], second[1]) 
					};
			
		} else if (value instanceof NeExpr) {
		
			NeExpr expr = (NeExpr) value;
			Value[] first = getNewValues(expr.getOp1()),
					second = getNewValues(expr.getOp2());
			return new Value[]{ 
					Jimple.v().newNeExpr(first[0], second[0]), 
					Jimple.v().newNeExpr(first[1], second[1]) 
					};
			
		}else if (value instanceof EqExpr) {
		
			EqExpr expr = (EqExpr) value;
			Value[] first = getNewValues(expr.getOp1()),
					second = getNewValues(expr.getOp2());
			return new Value[]{ 
					Jimple.v().newEqExpr(first[0], second[0]), 
					Jimple.v().newEqExpr(first[1], second[1]) 
					};
			
		} else if (value instanceof LeExpr) {
		
			LeExpr expr = (LeExpr) value;
			Value[] first = getNewValues(expr.getOp1()),
					second = getNewValues(expr.getOp2());
			return new Value[]{ 
					Jimple.v().newLeExpr(first[0], second[0]), 
					Jimple.v().newLeExpr(first[1], second[1]) 
					};
			
		} else if (value instanceof GeExpr) {
		
			GeExpr expr = (GeExpr) value;
			Value[] first = getNewValues(expr.getOp1()),
					second = getNewValues(expr.getOp2());
			return new Value[]{ 
					Jimple.v().newGeExpr(first[0], second[0]), 
					Jimple.v().newGeExpr(first[1], second[1]) 
					};
			
		} else if (value instanceof LtExpr) {
		
			LtExpr expr = (LtExpr) value;
			Value[] first = getNewValues(expr.getOp1()),
					second = getNewValues(expr.getOp2());
			return new Value[]{ 
					Jimple.v().newLtExpr(first[0], second[0]), 
					Jimple.v().newLtExpr(first[1], second[1]) 
					};
			
		} else if (value instanceof GtExpr) {
		
			GtExpr expr = (GtExpr) value;
			Value[] first = getNewValues(expr.getOp1()),
					second = getNewValues(expr.getOp2());
			return new Value[]{ 
					Jimple.v().newGtExpr(first[0], second[0]), 
					Jimple.v().newGtExpr(first[1], second[1]) 
					};
			
		} else if (value instanceof InstanceInvokeExpr) {
		
			InstanceInvokeExpr expr = (InstanceInvokeExpr) value;
			Value[] bases = getNewValues(expr.getBase());
			ArrayList<Value>[] args = (ArrayList<Value>[]) new ArrayList[]{ new ArrayList<Value>(), new ArrayList<Value>() };
					
			for(Value arg : expr.getArgs()) {
				Value[] newArgs = getNewValues(arg);
				args[0].add(newArgs[0]);
				args[1].add(newArgs[1]);
			}
			
			return new Value[]{ 
					Jimple.v().newVirtualInvokeExpr((Local) bases[0], expr.getMethodRef(), args[0]),
					Jimple.v().newVirtualInvokeExpr((Local) bases[1], expr.getMethodRef(), args[1]),
					};
		
		} else if (value instanceof StaticInvokeExpr) {

			InvokeExpr expr = (InvokeExpr) value;
			ArrayList<Value>[] args = (ArrayList<Value>[]) new ArrayList[]{ new ArrayList<Value>(), new ArrayList<Value>() };
					
			for(Value arg : expr.getArgs()) {
				Value[] newArgs = getNewValues(arg);
				args[0].add(newArgs[0]);
				args[1].add(newArgs[1]);
			}
			
			return new Value[]{ 
					Jimple.v().newStaticInvokeExpr(expr.getMethodRef(), args[0]),
					Jimple.v().newStaticInvokeExpr(expr.getMethodRef(), args[1]),
					};
		
		}
		System.out.println();
		return new Value[]{value, value};
	}
}
