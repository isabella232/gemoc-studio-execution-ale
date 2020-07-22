/*******************************************************************************
 * Copyright (c) 2020 Inria and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Inria - initial API and implementation
 *******************************************************************************/
package org.eclipse.gemoc.ale.interpreted.engine.sirius;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecoretools.ale.core.env.impl.AbstractAleEnvironment;
import org.eclipse.emf.ecoretools.ale.core.env.impl.ImmutableBehaviors;
import org.eclipse.emf.ecoretools.ale.core.parser.ParsedFile;
import org.eclipse.emf.ecoretools.ale.implementation.ModelUnit;

/**
 * ALE environment to be used by model being edited without knowledge of .dsl
 * (so without ALE source files, but only with the metamodels).
 * 
 * This is thus possible to use it on model in the Sirius editor (ie. model not
 * running)
 */
public class BasicEditionRawAleEnvironment extends AbstractAleEnvironment {

	private final List<EPackage> metamodels;

	public BasicEditionRawAleEnvironment(Collection<EPackage> metamodels) {
		List<ParsedFile<ModelUnit>> behaviors = new ArrayList<ParsedFile<ModelUnit>>();
		this.behaviors = new ImmutableBehaviors(behaviors);
		this.metamodels = new ArrayList<>(metamodels);
	}

	@Override
	public List<EPackage> getMetamodels() {
		return new ArrayList<>(metamodels);
	}

	@Override
	public LinkedHashSet<String> getBehaviorsSources() {
		return new LinkedHashSet<>(0);
	}

	@Override
	public LinkedHashSet<String> getMetamodelsSources() {
		return new LinkedHashSet<>(0);
	}

}
