/*
 * Copyright (C) 2016 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2016 University of Freiburg
 *
 * This file is part of the ULTIMATE AbstractInterpretationV2 plug-in.
 *
 * The ULTIMATE AbstractInterpretationV2 plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE AbstractInterpretationV2 plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE AbstractInterpretationV2 plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE AbstractInterpretationV2 plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE AbstractInterpretationV2 plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.util;

import java.util.Collections;

import de.uni_freiburg.informatik.ultimate.core.model.models.IBoogieType;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.NoopScript;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Theory;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.IBoogieVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TypeSortTranslator;

/**
 * A simulating implementation of {@link IBoogieVar}.
 *
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 *
 */
public final class TemporaryBoogieVar implements IBoogieVar {

	private static final Script SCRIPT_MOCK = new MockScript();

	private final IBoogieType mType;
	private final String mId;
	private final Sort mSort;

	public TemporaryBoogieVar(final IBoogieType type, final String identifier) {
		mType = type;
		mId = identifier;
		mSort = new TypeSortTranslator(Collections.emptyList(), SCRIPT_MOCK, null).constructSort(type, a -> null);
	}

	@Override
	public String getGloballyUniqueId() {
		return mId;
	}

	@Override
	public IBoogieType getIType() {
		return mType;
	}

	@Override
	public ApplicationTerm getDefaultConstant() {
		throw new UnsupportedOperationException("Temporary IBoogieVars dont have default constants.");
	}

	@Override
	public String toString() {
		return mId;
	}

	@Override
	public Sort getSort() {
		return mSort;
	}

	private static final class MockScript extends NoopScript {
		public MockScript() {
			super(new Theory(Logics.ALL));
		}
	}
}