package alu.linking.disambiguation;

/**
 * Interface for a type of scorer which is context-based and is meant to be
 * executed once a context exists
 * 
 * @author Kristian Noullet
 *
 * @param <T> scorer type
 * @param <M> context base type
 */
public interface PostScorer<T, M> extends Scorer<T>, ContextBase<M> {

}
