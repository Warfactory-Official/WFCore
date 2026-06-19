package wfcore.api.metatileentity;

/**
 * Policy describing how a machine leaves its current animation when the desired state changes.
 */
public enum AnimTransition {
    /** Switch to the new animation immediately. May visually snap. */
    SNAP,
    /** Keep playing the current animation until it reaches its (loop) end, then switch. Avoids snapping. */
    FINISH_LOOP
}
