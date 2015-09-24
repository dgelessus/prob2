package de.prob.model.eventb.algorithm.graph

import de.prob.model.eventb.algorithm.AlgorithmASTVisitor;
import de.prob.model.eventb.algorithm.Assertion
import de.prob.model.eventb.algorithm.AssignmentAnalysisVisitor;
import de.prob.model.eventb.algorithm.Assignments
import de.prob.model.eventb.algorithm.Block
import de.prob.model.eventb.algorithm.If
import de.prob.model.eventb.algorithm.Statement
import de.prob.model.eventb.algorithm.While


class PCCalculator extends AlgorithmASTVisitor {
	ControlFlowGraph graph
	Map<Statement, Integer> pcInformation
	int pc = 0
	boolean optimized

	def PCCalculator(ControlFlowGraph graph, boolean optimized) {
		this.graph = graph
		this.optimized = optimized
		pcInformation = [:]
		visit(graph.algorithm)
	}

	def visit(While s) {
		if (graph.nodes.contains(s)) {
			pcInformation[s] = pc++
		}
	}

	def visit(If s) {
		if (graph.nodes.contains(s)) {
			pcInformation[s] = pc++
		}
	}

	def visit(Assignments s) {
		if (!optimized && graph.nodes.contains(s)) {
			pcInformation[s] = pc++
		} else if(optimized && graph.entryNode == s) {
			pcInformation[s] = pc++
		} else if(optimized && !graph.inEdges(s).findAll { Edge e -> e.conditions.isEmpty() }.isEmpty()) {
			pcInformation[s] = pc++
		}
	}

	def visit(Assertion s) {
		// do nothing
	}
}