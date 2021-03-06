package com.toxicbakery.kfinstatemachine

import kotlin.reflect.KClass

open class StateMachine<S> : IStateMachine<S> {

    private val transitionRules: Array<out TransitionRule<S, *>>

    final override var state: S

    constructor(initialState: S, vararg transitionRules: TransitionRule<S, *>) {
        state = initialState
        this.transitionRules = transitionRules
    }

    constructor(initialState: S, transitions: List<TransitionRule<S, *>>) {
        state = initialState
        transitionRules = transitions.toTypedArray()
    }

    override val transitions: Set<KClass<*>>
        get() = transitionRules.filter { it.oldState == state }
                .map { it.transition }
                .toSet()

    override fun transition(transition: Any): Unit =
            edge(transition)
                    .also { state = it.newState }
                    .performReactions(this, transition)

    override fun transitionsTo(targetState: S): Set<KClass<*>> =
            transitionRules
                    .filter { rule: TransitionRule<S, *> ->
                        rule.oldState == state
                                && rule.newState == targetState
                    }
                    .map(TransitionRule<S, *>::transition)
                    .toSet()

    @Suppress("UNCHECKED_CAST")
    private fun edge(transition: Any): TransitionRule<S, *> = transitionRules
            .filter { transitionRule ->
                transitionRule.oldState == state
                        && transitionRule.transition.isInstance(transition)
                        && transitionRule.validate(transition)
            }
            .let { transitions: List<TransitionRule<S, *>> ->
                when {
                    transitions.isEmpty() ->
                        throw Exception("Invalid transition `$transition` for state `$state`.\nValid transitions ${this.transitions}")
                    transitions.size > 1 ->
                        throw Exception("Ambiguous transition `$transition` for state `$state`.\nMatches ${transitions.toTransitionsString()}.")
                    else -> transitions.first()
                }
            }

    companion object {
        private fun <S> List<TransitionRule<S, *>>.toTransitionsString(): String =
                joinToString(separator = "\n") { transitionRule ->
                    "${transitionRule.oldState} -> ${transitionRule.newState}"
                }

        fun <F, T : Any> transition(oldState: F, transition: KClass<T>, newState: F): TransitionRule<F, T> =
                TransitionRule(
                        oldState = oldState,
                        transition = transition,
                        newState = newState)
    }

}

data class TransitionRule<S, T : Any>(
        val oldState: S,
        val transition: KClass<T>,
        val newState: S,
        private val validations: List<(transition: T) -> Boolean> = listOf(),
        private val reactions: List<(machine: StateMachine<S>, transition: T) -> Unit> = listOf()
) {

    fun onlyIf(func: (transition: T) -> Boolean): TransitionRule<S, T> =
            copy(validations = validations.plus(func))

    fun reaction(func: (machine: StateMachine<S>, transition: T) -> Unit): TransitionRule<S, T> =
            copy(reactions = reactions.plus(func))

    @Suppress("UNCHECKED_CAST")
    internal fun validate(transition: Any) = validations
            .fold(true) { acc: Boolean, validation: (transition: T) -> Boolean ->
                acc && validation(transition as T)
            }

    @Suppress("UNCHECKED_CAST")
    internal fun performReactions(machine: StateMachine<S>, transition: Any) = reactions
            .forEach { func -> func(machine, transition as T) }

}
