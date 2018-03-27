package com.toxicbakery.kfinstatemachine

import com.toxicbakery.kfinstatemachine.RxMachineTest.Energy.Kinetic
import com.toxicbakery.kfinstatemachine.RxMachineTest.Energy.Potential
import com.toxicbakery.kfinstatemachine.RxMachineTest.EnergyTransition.Release
import com.toxicbakery.kfinstatemachine.RxMachineTest.EnergyTransition.Store
import com.toxicbakery.kfinstatemachine.graph.DirectedGraph
import com.toxicbakery.kfinstatemachine.graph.GraphEdge
import org.junit.Assert.assertEquals
import org.junit.Test

class RxMachineTest {

    sealed class Energy(override val id: String) : FiniteState {
        object Kinetic : Energy("kinetic")
        object Potential : Energy("potential")
    }

    sealed class EnergyTransition(override val event: String) : Transition {
        object Store : EnergyTransition("Store")
        object Release : EnergyTransition("Release")
    }

    private val energyDirectedGraph = DirectedGraph(
            edges = setOf(
                    GraphEdge(
                            left = Potential,
                            right = Kinetic,
                            label = Release
                    ),
                    GraphEdge(
                            left = Kinetic,
                            right = Potential,
                            label = Store
                    )
            )
    )

    @Test
    fun stateObservableTest() {
        val machine = BaseMachine(energyDirectedGraph, Potential)
        val expectedStates = mutableListOf(
                Potential,
                Kinetic
        )

        val disposable = machine.stateObservable
                .subscribe { energy: Energy -> assertEquals(expectedStates.removeAt(0), energy) }

        machine.performTransition(Release)
        assertEquals(0, expectedStates.size)
        disposable.dispose()
    }

    @Test
    fun transitionObservableTest() {
        val machine = BaseMachine(energyDirectedGraph, Potential)
        val expectedTransitions = mutableListOf(
                TransitionEvent(Release, Kinetic),
                TransitionEvent(Store, Potential)
        )

        val disposable = machine.transitionObservable
                .subscribe { transitionEvent -> assertEquals(expectedTransitions.removeAt(0), transitionEvent) }

        machine.performTransition(Release)
        machine.performTransition(Store)
        assertEquals(0, expectedTransitions.size)
        disposable.dispose()
    }

}