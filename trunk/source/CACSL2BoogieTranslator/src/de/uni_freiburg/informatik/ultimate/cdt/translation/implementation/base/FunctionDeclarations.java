/*
 * Copyright (C) 2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Thomas Lang
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE CACSL2BoogieTranslator plug-in.
 * 
 * The ULTIMATE CACSL2BoogieTranslator plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE CACSL2BoogieTranslator plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE CACSL2BoogieTranslator plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE CACSL2BoogieTranslator plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE CACSL2BoogieTranslator plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base;

import java.util.LinkedHashMap;

import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.cHandler.TypeSizes;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive.PRIMITIVE;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.SFO;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.handler.ITypeHandler;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.ASTType;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Attribute;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.FunctionDeclaration;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.PrimitiveType;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.VarList;
import de.uni_freiburg.informatik.ultimate.model.location.ILocation;

/**
 * Collect function declarations for all functions that are used in the
 * translation.
 * During the translation we use one object of this class. The object provides
 * methods to construct function declarations. At the end of the translation
 * process all these function declarations should be added to the resulting
 * Boogie program.
 * @author Matthias Heizmann
 *
 */
public class FunctionDeclarations {
	
	/**
	 * Names of all bitwise operation that occurred in the program.
	 */
	private final LinkedHashMap<String, FunctionDeclaration> m_DeclaredFunctions = new LinkedHashMap<String, FunctionDeclaration>();
	private final ITypeHandler m_TypeHandler;
	private final TypeSizes m_TypeSizeConstants;
	public static final String s_BUILTIN_IDENTIFIER = "builtin";
	public static final String s_OVERAPPROX_IDENTIFIER = "overapproximation";
	public static final String s_INDEX_IDENTIFIER = "indices";
	
	
	public FunctionDeclarations(ITypeHandler typeHandler,
			TypeSizes typeSizeConstants) {
		super();
		m_TypeHandler = typeHandler;
		m_TypeSizeConstants = typeSizeConstants;
	}

	public void declareFunction(ILocation loc, String prefixedFunctionName, Attribute[] attributes, 
			boolean boogieResultTypeBool, CPrimitive resultCType, CPrimitive... paramCTypes) {
		final ASTType resultASTType;
		if (boogieResultTypeBool) {
			resultASTType = new PrimitiveType(loc, "bool");
		} else {
			resultASTType = m_TypeHandler.ctype2asttype(loc, resultCType);
		}
		ASTType[] paramASTTypes = new ASTType[paramCTypes.length];
		for (int i=0; i<paramCTypes.length; i++) {
			paramASTTypes[i] = m_TypeHandler.ctype2asttype(loc, paramCTypes[i]);
		}
		declareFunction(loc, prefixedFunctionName, attributes, resultASTType, paramASTTypes);
	}
	
	public void declareFunction(ILocation loc, String prefixedFunctionName, Attribute[] attributes, ASTType resultASTType, ASTType... paramASTTypes) {
		if (m_DeclaredFunctions.containsKey(prefixedFunctionName)) {
			return;
			//throw new IllegalArgumentException("Function " + functionName + " already declared");
		}
		if (!prefixedFunctionName.startsWith(SFO.AUXILIARY_FUNCTION_PREFIX)) {
			throw new IllegalArgumentException("Our convention says that user defined functions start with tilde");
		}

		VarList[] inParams = new VarList[paramASTTypes.length];
		for (int i=0; i<paramASTTypes.length; i++) {
			inParams[i] = new VarList(loc, new String[] { "in" + i }, paramASTTypes[i]);
		}
		VarList outParam = new VarList(loc, new String[] { "out" }, resultASTType);
		FunctionDeclaration d = new FunctionDeclaration(loc, attributes, prefixedFunctionName, new String[0], inParams, outParam);
		m_DeclaredFunctions.put(prefixedFunctionName, d);
	}

	public LinkedHashMap<String, FunctionDeclaration> getDeclaredFunctions() {
		return m_DeclaredFunctions;
	}
	
	public String computeBitvectorSuffix(ILocation loc, CPrimitive... paramCTypes) {
		CPrimitive firstParam = paramCTypes[0];
		Integer bytesize = m_TypeSizeConstants.getSize(firstParam.getType());
		int bitsize = bytesize * 8;
		
		return String.valueOf(bitsize);
	}
	
	
	/**
	 * Check if all CPrimitives in a sequence are equivalent.
	 * @return true iff all CPrimitives in cPrimitives are equivalent.
	 */
	public boolean checkParameters(CPrimitive... cPrimitives) {
		PRIMITIVE type = cPrimitives[0].getType();
		for (CPrimitive t : cPrimitives) {
			if (!t.getType().equals(type)) {
				return false;
			}
		}
		return true;
	}



}
