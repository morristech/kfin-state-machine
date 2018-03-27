package com.toxicbakery.kfinstatemachine.xml

import com.toxicbakery.kfinstatemachine.BaseMachine
import com.toxicbakery.kfinstatemachine.FiniteState
import com.toxicbakery.kfinstatemachine.StateMachine
import com.toxicbakery.kfinstatemachine.Transition
import com.toxicbakery.kfinstatemachine.graph.DirectedGraph
import com.toxicbakery.kfinstatemachine.xml.model.XmlRoot
import com.toxicbakery.kfinstatemachine.xml.model.XmlState

fun XmlRoot.createSimpleMachine() =
        createSimpleMachineWithMapping(
                stateMapper = { id -> FiniteXmlState(id) },
                transitionMapper = { event -> FiniteXmlTransition(event) }
        )

/**
 * Create a simple machine from this root node.
 */
fun <F : FiniteState, T : Transition> XmlRoot.createSimpleMachineWithMapping(
        stateMapper: (id: String) -> F,
        transitionMapper: (event: String) -> T
): StateMachine<F, T> = mutableMapOf<F, MutableMap<T, F>>()
        .apply {
            states.forEach { xmlState ->
                xmlState.transitions
                        .forEach { xmlTransition ->
                            val left = stateMapper(xmlState.id)
                            val right = stateMapper(xmlTransition.target)
                            val edge = transitionMapper(xmlTransition.event)
                            getOrPut(left, { mutableMapOf() })[edge] = right
                        }
            }
        }
        .toMap()
        .let { mappedEdges ->
            BaseMachine(
                    directedGraph = DirectedGraph(mappedEdges),
                    initialState = stateMapper(initial)
            )
        }

/**
 * Create a parallel machine from this root node and all sub machines.
 */
fun XmlRoot.createParallelMachines() =
        mutableMapOf<String, StateMachine<FiniteState, Transition>>()
                .also { namedMachines -> createParallelMachines(namedMachines, parallel) }

internal fun createParallelMachines(
        namedMachines: MutableMap<String, StateMachine<FiniteState, Transition>>,
        parallel: List<XmlState>
) = parallel
        .flatMap(XmlState::states)
        .forEach { xmlState -> xmlState.createParallelMachine(namedMachines) }

private fun XmlState.createParallelMachine(
        namedMachines: MutableMap<String, StateMachine<FiniteState, Transition>>
) {
    states.forEach { }
    namedMachines[id] = BaseMachine(
            directedGraph = DirectedGraph<FiniteState, Transition>(mapOf()),
            initialState = FiniteXmlState("")
    )
}
