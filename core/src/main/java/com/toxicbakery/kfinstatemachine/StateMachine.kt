package com.toxicbakery.kfinstatemachine

import kotlin.reflect.KClass

open class StateMachine<S>(
        initialState: S,
        private vararg val transitionRules: TransitionRule<S, *>
) : IStateMachine<S> {

    private var _state: S = initialState

    private val edges: Set<TransitionRule<S, *>>
        get() = transitionRules
                .filter { transitionRule -> transitionRule.oldState == _state }
                .toSet()

    override val state: S
        get() = _state

    override val transitions: Set<KClass<*>>
        get() = transitionRules.filter { it.oldState == _state }
                .map { it.transition }
                .toSet()

    override fun transition(transition: Any): Unit =
            edge(transition)
                    .also { _state = it.newState }
                    .performReactions(this, transition)

    override fun transitionsTo(targetState: S): Set<KClass<*>> =
            transitionRules
                    .filter { rule: TransitionRule<S, *> ->
                        rule.oldState == _state
                                && rule.newState == targetState
                    }
                    .map(TransitionRule<S, *>::transition)
                    .toSet()

    @Suppress("UNCHECKED_CAST")
    private fun edge(transition: Any) = edges
            .singleOrNull { it.transition.java.isInstance(transition) && it.validate(transition) }
            ?: throw Exception("Invalid transition `${transition.javaClass.simpleName}` for state `$_state`.")

    companion object {
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
            .fold(true, { acc: Boolean, validation: (transition: T) -> Boolean ->
                acc && validation(transition as T)
            })

    @Suppress("UNCHECKED_CAST")
    internal fun performReactions(machine: StateMachine<S>, transition: Any) = reactions
            .forEach { func -> func(machine, transition as T) }

}