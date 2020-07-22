/*******************************************************************************
 * Copyright (c) 2018, 2020 Inria and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *     Inria - refactoring and improvements
 *******************************************************************************/
package org.eclipse.gemoc.ale.interpreted.engine.sirius;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.acceleo.query.runtime.EvaluationResult;
import org.eclipse.acceleo.query.runtime.IQueryBuilderEngine;
import org.eclipse.acceleo.query.runtime.IQueryBuilderEngine.AstResult;
import org.eclipse.acceleo.query.runtime.IQueryEnvironment;
import org.eclipse.acceleo.query.runtime.IQueryEvaluationEngine;
import org.eclipse.acceleo.query.runtime.QueryEvaluation;
import org.eclipse.acceleo.query.runtime.QueryParsing;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecoretools.ale.core.interpreter.impl.AleInterpreter;
import org.eclipse.emf.ecoretools.ale.core.interpreter.impl.OptimizedEvaluationResult;
import org.eclipse.gemoc.ale.interpreted.engine.AleEngine;
import org.eclipse.sirius.common.acceleo.aql.business.internal.AQLSiriusInterpreter;
import org.eclipse.sirius.common.tools.api.interpreter.EvaluationException;
import org.eclipse.sirius.common.tools.api.interpreter.IEvaluationResult;
import org.eclipse.xtext.EcoreUtil2;

public class ALESiriusInterpreter extends AQLSiriusInterpreter {

	/**
	 * list of known engines that can be used to evalutate the expressions
	 */
	HashSet<AleEngine> knownEngines = new HashSet<>();

	// ALESiriusInterpreter is a singleton
	private static ALESiriusInterpreter instance = new ALESiriusInterpreter();

	private ALESiriusInterpreter() {
	}

	public static ALESiriusInterpreter getDefault() {
		return instance;
	}

	@Override
	public boolean provides(String expression) {
		return expression != null && expression.startsWith("ale:");
	}

	@Override
	public IEvaluationResult evaluateExpression(final EObject target, final String fullExpression)
			throws EvaluationException {
		this.javaExtensions.reloadIfNeeded();
		String expression = fullExpression.replaceFirst("ale:", "");
		Map<String, Object> variables = getVariables();
		variables.put("self", target); //$NON-NLS-1$

		IQueryEnvironment queryEnv = getQueryEnvironment(target);
		final IQueryBuilderEngine builder = QueryParsing.newBuilder(queryEnv);
		AstResult build = builder.build(expression);
		IQueryEvaluationEngine evaluationEngine = QueryEvaluation.newEngine(queryEnv);
		final EvaluationResult evalResult = evaluationEngine.eval(build, variables);

		final BasicDiagnostic diagnostic = new BasicDiagnostic();
		if (Diagnostic.OK != build.getDiagnostic().getSeverity()) {
			diagnostic.merge(build.getDiagnostic());
		}
		if (Diagnostic.OK != evalResult.getDiagnostic().getSeverity()) {
			diagnostic.merge(evalResult.getDiagnostic());
		}
		if (queryEnv instanceof BasicEditionRawAleEnvironment) {
			// release resources
			((BasicEditionRawAleEnvironment) queryEnv).close();
		}
		return new OptimizedEvaluationResult(Optional.ofNullable(evalResult.getResult()), diagnostic);
	}

	/**
	 * retrieve the queryEnvironment from the associated engine or create a
	 * BasicEditionRawAleEnvironment (for edition mode) Important:
	 * BasicEditionRawAleEnvironment must be closed after use, in order to avoid
	 * memory leak
	 */
	@SuppressWarnings("resource")
	private IQueryEnvironment getQueryEnvironment(final EObject target) {
		Optional<AleInterpreter> aleInterpreter = findInterpreterForModel(target);
		if (aleInterpreter.isPresent() && aleInterpreter.get().getQueryEnvironment() != null) {
			return aleInterpreter.get().getQueryEnvironment();
		} else {
			List<EPackage> metamodels = getMetamodels(target);
			IQueryEnvironment queryEnv = new BasicEditionRawAleEnvironment(metamodels).getContext();
			for (EPackage pack : metamodels) {
				queryEnv.registerEPackage(pack);
			}
			return queryEnv;
		}
	}

	/**
	 * Look in known engines that are running the model containing the given target
	 * EObject
	 * 
	 * @param target
	 * @return the AleInterpreter of the found AleEngine
	 */
	protected Optional<AleInterpreter> findInterpreterForModel(final EObject target) {
		for (AleEngine engine : knownEngines) {
			if (engine.getExecutionContext().getResourceModel().getResourceSet() == target.eResource()
					.getResourceSet()) {
				return Optional.ofNullable(engine.getInterpreter());
			}
		}
		return Optional.empty();
	}

	/**
	 * reflectively find the metamodels of the given EObject
	 * 
	 * Ie. look for all EPackage of all Resources in the ResourceSet
	 * 
	 * @param target
	 * @return a list of EPackage
	 */
	protected List<EPackage> getMetamodels(final EObject target) {
		ArrayList<EPackage> metamodels = new ArrayList<EPackage>();
		for (Resource res : target.eResource().getResourceSet().getResources()) {
			// TODO find a more efficient request ? (ie. avoid to navigate the whole model )
			// could we make the assumption that EPackage are only at the root or contained
			// by EPackage ?
			// (DVK: I know that UML may use EPackage in order to create profiles
			// instantiation)
			metamodels.addAll(EcoreUtil2.typeSelect(EcoreUtil2.eAllContentsAsList(res), EPackage.class));
		}
		return metamodels;
	}

	public void addAleEngine(AleEngine engine) {
		knownEngines.add(engine);
	}

	public void removeAleEngine(AleEngine engine) {
		knownEngines.remove(engine);
	}
}
