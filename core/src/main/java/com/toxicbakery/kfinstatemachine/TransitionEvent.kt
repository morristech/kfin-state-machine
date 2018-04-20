package com.toxicbakery.kfinstatemachine

/**
 * An event describing a transition to a new state.
 *
 * @param transition event that triggered the state change
 * @param targetState the state that the machine will end in once the transition is complete
 */
data class TransitionEvent<out F>(
        val transition: Any,
        val targetState: F
)